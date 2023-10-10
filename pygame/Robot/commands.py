import math
from abc import ABC, abstractmethod
from settings import *
from Map import *


class Command(ABC):
    def __init__(self, time):
        self.time = time  # Time in seconds in which this command is carried out.
        self.ticks = math.ceil(time * FRAMES)  # Number of frame ticks that this command will take.
        self.total_ticks = self.ticks  # Keep track of original total ticks.

    def tick(self):
        self.ticks -= 1

    @abstractmethod
    def process_one_tick(self, robot):
        """
        Overriding method must call tick().
        """
        pass

    @abstractmethod
    def apply_on_pos(self, curr_pos):
        """
        Apply this command to a Position, such that its attributes will reflect the correct values
        after the command is done.

        This method should return itself.
        """
        pass

    @abstractmethod
    def convert_to_message(self):
        """
        Conversion to a message that is easy to send over the RPi.
        """
        pass


class ScanCommand(Command):
    def __init__(self, time, obj_index):
        super().__init__(time)
        self.obj_index = obj_index

    def __str__(self):
        return f"ScanCommand(time={self.time, self.obj_index})"

    __repr__ = __str__

    def process_one_tick(self, robot):
        if self.total_ticks == 0:
            return

        self.tick()

    def apply_on_pos(self, curr_pos):
        pass

    def convert_to_message(self):
        # Just return a string of s's
        # return f"S{self.obj_index:01}"

        return f"SNAP{self.obj_index}_C"


class StraightCommand(Command):
    def __init__(self, dist):
        """
        Specified distance is scaled. Do not divide the provided distance by the scaling factor!
        """
        # Calculate the time needed to travel the required distance.
        time = abs(dist / ROBOT_SPEED_PER_SECOND)
        super().__init__(time)

        self.dist = dist

    def __str__(self):
        return f"StraightCommand(dist={self.dist / SCALING_FACTOR}, {self.total_ticks} ticks)"

    __repr__ = __str__

    def process_one_tick(self, robot):
        if self.total_ticks == 0:
            return

        self.tick()
        distance = self.dist / self.total_ticks
        robot.straight(distance)

    def apply_on_pos(self, curr_pos: Position):
        """
        Apply this command onto a current Position object.
        """
        if curr_pos.direction == Direction.RIGHT:
            curr_pos.x += self.dist
        elif curr_pos.direction == Direction.TOP:
            curr_pos.y += self.dist
        elif curr_pos.direction == Direction.BOTTOM:
            curr_pos.y -= self.dist
        else:
            curr_pos.x -= self.dist

        return self

    def convert_to_message(self):
        # MESSAGE: fXXXX for forward, bXXXX for backward.
        # XXXX is the distance in decimal in centimeters.

        # Note that the distance is now scaled.
        # Therefore, we need to de-scale it.
        descaled_distance = int(self.dist // SCALING_FACTOR)
        # Check if forward or backward.
        if descaled_distance < 0:
            # It is a backward command.
            return f"BW{abs(descaled_distance):03}"
        # Else, it is a forward command.
        return f"FW{descaled_distance:03}"


class TurnCommand(Command):
    def __init__(self, angle, rev):
        """
        Angle to turn and whether the turn is done in reverse or not. Note that this is in degrees.

        Note that negative angles will always result in the robot being rotated clockwise.
        """
        time = abs((math.radians(angle) * ROBOT_LENGTH) /
                   (ROBOT_SPEED_PER_SECOND * ROBOT_S_FACTOR))
        super().__init__(time)

        self.angle = angle
        self.rev = rev

    def __str__(self):
        return f"TurnCommand({self.angle:.2f}degrees, {self.total_ticks} ticks, rev={self.rev})"

    __repr__ = __str__

    def process_one_tick(self, robot):
        if self.total_ticks == 0:
            return

        self.tick()
        angle = self.angle / self.total_ticks
        robot.turn(angle, self.rev)

    def apply_on_pos(self, curr_pos: Position):
        """
        x_new = x + R(sin(∆θ + θ) - sin θ)
        y_new = y - R(cos(∆θ + θ) - cos θ)
        θ_new = θ + ∆θ
        R is the turning radius.

        Take note that:
            - +ve ∆θ -> rotate counter-clockwise
            - -ve ∆θ -> rotate clockwise

        Note that ∆θ is in radians.
        """
        assert isinstance(curr_pos, RobotPosition), print("Cannot apply turn command on non-robot positions!")

         if (self.angle < 0 and 45 < curr_pos.angle <= 90 and not self.rev) or \
           (self.angle < 0 and 45 < curr_pos.angle <= 90 and self.rev) or \
           (self.angle < 0 and -45 < curr_pos.angle <= 45 and not self.rev) or \
           (self.angle < 0 and -45 < curr_pos.angle <= 45 and self.rev) or \
           (self.angle > 0 and curr_pos.angle <= -125 and self.rev) or \
           (self.angle < 0 and curr_pos.angle <= -125 and self.rev) or \
           (self.angle > 0 and -45 < curr_pos.angle <= 45 and self.rev):
          
            x = 25
            y = -25

        else:
            x = -25
            y = 25

        # Get change in (x, y) coordinate.
        x_change = (125+x)  * (math.sin(math.radians(curr_pos.angle + self.angle)) -
                                                 math.sin(math.radians(curr_pos.angle)))
        y_change = (125+y) * (math.cos(math.radians(curr_pos.angle + self.angle)) -
                                                 math.cos(math.radians(curr_pos.angle)))


        if self.angle < 0 and not self.rev:  # Wheels to right moving forward.
            curr_pos.x -= x_change
            curr_pos.y += y_change
        elif (self.angle < 0 and self.rev) or (self.angle >= 0 and not self.rev):
            # (Wheels to left moving backwards) or (Wheels to left moving forwards).
            curr_pos.x += x_change
            curr_pos.y -= y_change
        else:  # Wheels to right moving backwards.
            curr_pos.x -= x_change
            curr_pos.y += y_change
        curr_pos.angle += self.angle

        if curr_pos.angle < -180:
            curr_pos.angle += 2 * 180
        elif curr_pos.angle >= 180:
            curr_pos.angle -= 2 * 180

        # Update the Position's direction.
        if 45 <= curr_pos.angle <= 3 * 45:
            curr_pos.direction = Direction.TOP
        elif -45 < curr_pos.angle < 45:
            curr_pos.direction = Direction.RIGHT
        elif -45 * 3 <= curr_pos.angle <= -45:
            curr_pos.direction = Direction.BOTTOM
        else:
            curr_pos.direction = Direction.LEFT
        return self

    def convert_to_message(self):
        if self.angle > 0 and not self.rev:
            # This is going forward left.
            return "FL090"  # Note the smaller case L.
        elif self.angle > 0 and self.rev:
            # This is going backward and with the wheels to the right.
            return "BR090"
        elif self.angle < 0 and not self.rev:
            # This is going forward right.
            return "FR090"
        else:
            # This is going backward and with the wheels to the left.
            return "BL090"
