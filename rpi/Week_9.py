#!/usr/bin/env python3
import io
import json
import queue
import time
from multiprocessing import Process, Manager
import picamera
from typing import Optional
import requests
from communication.android import AndroidLink, AndroidMessage
from communication.stm32 import STMLink
from consts import SYMBOL_MAP
from logger import prepare_logger
from settings import API_IP, API_PORT

class RaspberryPi:
    def __init__(self):
        # Initialize logger and communication objects with Android and STM
        self.logger = prepare_logger()
        self.android_link = AndroidLink()
        self.stm_link = STMLink()

        # For sharing information between child processes
        self.manager = Manager()

        # Events
        self.android_dropped = self.manager.Event()  # Set when the android link drops
        # commands will be retrieved from commands queue when this event is set
        self.unpause = self.manager.Event()

        # Movement Lock
        self.movement_lock = self.manager.Lock()

        # Queues
        self.android_queue = self.manager.Queue() # Messages to send to Android
        #self.rpi_action_queue = self.manager.Queue() # Messages that need to be processed by RPi
        self.command_queue = self.manager.Queue() # Messages that need to be processed by STM32, as well as snap commands

        # Define empty processes
        self.proc_recv_android = None
        self.proc_recv_stm32 = None
        self.proc_android_sender = None
        self.proc_command_follower = None
        #self.proc_rpi_action = None

        #self.near_flag = self.manager.Lock()

    def start(self):
        """Starts the RPi orchestrator"""
        try:
            # Establish Bluetooth connection with Android
            self.android_link.connect()
            self.android_queue.put(AndroidMessage('info', 'You are connected to the RPi!'))

            # Establish connection with STM32
            self.stm_link.connect()

            # Check Image Recognition and Algorithm API status
            self.check_api()
            self.snap_and_rec("None_C", True)
            
            #self.small_direction = self.snap_and_rec("Small")
            #self.logger.info(f"PREINFER small direction is: {self.small_direction}")

            # Define child processes
            self.proc_recv_android = Process(target=self.recv_android)
            self.proc_recv_stm32 = Process(target=self.recv_stm)
            self.proc_android_sender = Process(target=self.android_sender)
            self.proc_command_follower = Process(target=self.command_follower)
            #self.proc_rpi_action = Process(target=self.rpi_action)

            # Start child processes
            self.proc_recv_android.start()
            self.proc_recv_stm32.start()
            self.proc_android_sender.start()
            self.proc_command_follower.start()
            #self.proc_rpi_action.start()

            self.logger.info("Child Processes started")

            ### Start up complete ###

            # Send success message to Android
            self.android_queue.put(AndroidMessage('info', 'Robot is ready!'))
            
            # Handover control to the Reconnect Handler to watch over Android connection
            self.reconnect_android()

        except KeyboardInterrupt:
            self.stop()

    def stop(self):
        """Stops all processes on the RPi and disconnects gracefully with Android and STM32"""
        self.android_link.disconnect()
        self.stm_link.disconnect()
        self.logger.info("Program exited!")

    def reconnect_android(self):
        """Handles the reconnection to Android in the event of a lost connection."""
        self.logger.info("Reconnection handler is watching...")

        while True:
            # Wait for android connection to drop
            self.android_dropped.wait()

            self.logger.error("Android link is down!")

            # Kill child processes
            self.logger.debug("Killing android child processes")
            self.proc_android_sender.kill()
            self.proc_recv_android.kill()

            # Wait for the child processes to finish
            self.proc_android_sender.join()
            self.proc_recv_android.join()
            assert self.proc_android_sender.is_alive() is False
            assert self.proc_recv_android.is_alive() is False
            self.logger.debug("Android child processes killed")

            # Clean up old sockets
            self.android_link.disconnect()

            # Reconnect
            self.android_link.connect()

            # Recreate Android processes
            self.proc_recv_android = Process(target=self.recv_android)
            self.proc_android_sender = Process(target=self.android_sender)

            # Start previously killed processes
            self.proc_recv_android.start()
            self.proc_android_sender.start()

            self.logger.info("Android child processes restarted")
            self.android_queue.put(AndroidMessage("info", "You are reconnected!"))

            self.android_dropped.clear()
            
    
    def recv_android(self) -> None:
        """
        [Child Process] Processes the messages received from Android
        """
       
        while True:
            msg_str: Optional[str] = None
            try:
                msg_str = self.android_link.recv()
            except OSError:
                self.android_dropped.set()
                self.logger.debug("Event set: Android connection dropped")

                break

            # If an error occurred in recv()
            if msg_str is None:
                continue

            message: dict = json.loads(msg_str)

            ## Command: Start Moving ##
            if message['cat'] == "control":
                if message['value'] == "start":
        
                    if not self.check_api():
                        self.logger.error("API is down! Start command aborted.")
                        continue

                    self.clear_queues()

                    self.command_queue.put("M1000")
                    self.logger.info("Start command received, starting robot on Week 9 task!")
                    self.android_queue.put(AndroidMessage('info', 'running'))

                    # Commencing path following | Main trigger to start movement #
                    self.unpause.set()
                    
    def recv_stm(self) -> None:
        """
        [Child Process] Receive acknowledgement messages from STM32, and release the movement lock
        """
        self.ack_count = 0

        while True:
            message: str = self.stm_link.recv()
            # Acknowledgement from STM32
            if message.startswith("ACK"):
                time.sleep(0.1) # can reduce this?

                self.ack_count += 1

                # Release movement lock
                try:
                    self.movement_lock.release()
                except Exception:
                    self.logger.warning("Tried to release a released lock!")

                self.logger.debug(f"ACK from STM32 received, ACK count now:{self.ack_count}")
                
                self.logger.info(f"self.ack_count: {self.ack_count}")

                # Decision for smaller obstacles
                if self.ack_count == 1:
                    self.logger.debug("1st ACK received, robot reached smaller obstacle!")
                    self.small_direction = self.snap_and_rec("Small")
                    if self.small_direction == "Left Arrow": 
                        self.command_queue.put("M2000")
                        self.logger.debug("Going left for smaller obstacle!")
                    elif self.small_direction == "Right Arrow":
                        self.command_queue.put("M2001")
                        self.logger.debug("Going right for smaller obstacle!")
                    else:
                        self.command_queue.put("M2000")
                        self.logger.debug("Failed smaller one, going left by default!")
                
                # Decision for larger obstacles
                if self.ack_count == 2:
                    self.logger.debug("2nd ACK received, robot reached larger obstacle!")
                    self.large_direction = self.snap_and_rec("Large")
                    if self.large_direction == "Left Arrow": 
                        self.command_queue.put("M3000")
                        self.logger.debug("Going left for larger obstacle!")
                    elif self.large_direction == "Right Arrow":
                        self.command_queue.put("M3001")
                        self.logger.debug("Going right for larger obstacle!")
                    else:
                        self.command_queue.put("M3000")
                        self.logger.debug("Failed larger one, going right by default!")

                # Movement complete
                if self.ack_count == 3:
                    self.logger.debug("3rd ACK received from STM32!")
                    self.command_queue.put("FIN")
                    self.ack_count = 0

            else:
                self.logger.warning(
                    f"Ignored unknown message from STM: {message}")

    def android_sender(self) -> None:
        while True:
            try:
                message: AndroidMessage = self.android_queue.get(timeout=0.5)
            except queue.Empty:
                continue

            try:
                self.android_link.send(message)
            except OSError:
                self.android_queue.put(message)
                self.android_dropped.set()
                self.logger.debug("Event set: Android dropped")

                break

    def command_follower(self) -> None:
        while True:
            command: str = self.command_queue.get()
            self.unpause.wait()
            self.movement_lock.acquire()
            # M1000; M2000 / M2001; M3000 / M3001
            stm32_prefixes = ("M") 
            if command.startswith(stm32_prefixes):
                self.stm_link.send(command)
            elif command == "FIN":
                self.unpause.clear()
                self.movement_lock.release()
                self.logger.info("Commands queue finished.")
                self.android_queue.put(AndroidMessage("info", "Commands queue finished."))
                self.android_queue.put(AndroidMessage("info", "finished"))
                self.request_stitch()
            else:
                raise Exception(f"Unknown command: {command}")

    def snap_and_rec(self, obstacle_id: str, warm_up: bool = False) -> str:
        """
        RPi snaps an image and calls the API for image-rec.
        The response is then forwarded back to the android
        :param obstacle_id: the current obstacle ID
        """
        
        self.logger.info(f"Capturing image for obstacle id: {obstacle_id}")
        signal = "C"
        self.android_queue.put(AndroidMessage("info", f"Capturing image for obstacle id: {obstacle_id}"))
        url = f"http://{API_IP}:{API_PORT}/image"
        filename = f"{int(time.time())}_{obstacle_id}_{signal}.jpg"
        
        # capture an image
        if (not warm_up):
            stream = io.BytesIO()
            with picamera.PiCamera() as camera:
                camera.start_preview()
                time.sleep(0.1) # Try reduce this
                camera.capture(stream, format='jpeg')
            image_data = stream.getvalue()
        else:
            image_data = 0
            with open("Blank.jpg", "rb") as image:
                f = image.read()
                b = bytearray(f)
                image_data = b

        self.logger.debug("Requesting from image API")
        response = requests.post(
            url, files={"file": (filename, image_data)}) 
        
        if response.status_code != 200:
            self.logger.error(
                "Something went wrong when requesting path from image-rec API. Please try again.")
            return

        results = json.loads(response.content)
            
        ans = SYMBOL_MAP.get(results['image_id'])
        self.logger.info(f"Image recognition results for {obstacle_id}: {ans}")
        self.android_queue.put(AndroidMessage("info", f"Image recognition results for {obstacle_id}: {ans}"))

        return ans

    def request_stitch(self):
        url = f"http://{API_IP}:{API_PORT}/stitch"
        response = requests.get(url)
        if response.status_code != 200:
            self.logger.error("Something went wrong when requesting stitch from the API.")
            return
        self.logger.info("Images stitched!")

    def clear_queues(self):
        while not self.command_queue.empty():
            self.command_queue.get()

    def check_api(self) -> bool:
        url = f"http://{API_IP}:{API_PORT}/status"
        try:
            response = requests.get(url, timeout=1)
            if response.status_code == 200:
                self.logger.debug("API is up!")
                return True
        except ConnectionError:
            self.logger.warning("API Connection Error")
            return False
        except requests.Timeout:
            self.logger.warning("API Timeout")
            return False
        except Exception as e:
            self.logger.warning(f"API Exception: {e}")
            return False

if __name__ == "__main__":
    rpi = RaspberryPi()
    rpi.start()