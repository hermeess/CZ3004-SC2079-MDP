import pygame 
import datetime
import math
from enum import Enum
import itertools
from typing import Tuple
from Map import RobotPosition, Grid, Obstacle


class Direction(Enum):
    LEFT = 180
    TOP = 90
    BOTTOM = -90
    RIGHT = 0
  
RED = (255, 0, 0)
GREEN = (0, 255, 0)
BLUE = (0, 0, 255)

BLACK = (0, 0, 0)
WHITE = (255, 255, 255)

DARK_GREEN = (0, 100, 0)
GREY = (220, 220, 220)
DARK_YELLOW = (236, 183, 83)

PINK = (255, 51, 255)
PURPLE = (153, 51, 255)
DARK_BLUE = (51, 51, 255)
ORANGE = (255, 153, 51)
DARK_GREY = '#6B6B6B'
DARK_BLACK = '#1a1e24'
  
# PyGame settings
SCALING_FACTOR = 5
FRAMES = 60
WINDOW_SIZE = 1300, 1000

# Robot Attributes
ROBOT_LENGTH = 20 * SCALING_FACTOR  # Recommended robot footprint is 30cm by 30cm
ROBOT_TURN_RADIUS = 30 * SCALING_FACTOR
ROBOT_SPEED_PER_SECOND = 30 * SCALING_FACTOR
ROBOT_S_FACTOR = ROBOT_LENGTH / ROBOT_TURN_RADIUS  
ROBOT_SAFETY_DISTANCE = 15 * SCALING_FACTOR
ROBOT_SCAN_TIME = 0.25  # Time provided for scanning an obstacle image in seconds.

# Grid Attributes
GRID_LENGTH = 200 * SCALING_FACTOR  # Movement area is 200cm by 200cm
GRID_CELL_LENGTH = 10 * SCALING_FACTOR  # Grid cell is 10cm by 10cm
GRID_START_BOX_LENGTH = 30 * SCALING_FACTOR  # Recommended starting area is 40cm by 40cm
GRID_NUM_GRIDS = GRID_LENGTH // GRID_CELL_LENGTH  # Number of grid cells

# Obstacle Attributes
OBSTACLE_LENGTH = 10 * SCALING_FACTOR  # Obstacle is 10cm by 10cm
OBSTACLE_SAFETY_WIDTH = ROBOT_SAFETY_DISTANCE // 3 * 4  # With respect to the center of the obstacle

# Path Finding Attributes
PATH_TURN_COST = 999 * ROBOT_SPEED_PER_SECOND * ROBOT_TURN_RADIUS
# NOTE: Higher number == Lower Granularity == Faster Checking.
# Must be an integer more than 0! Number higher than 3 not recommended.
PATH_TURN_CHECK_GRANULARITY = 1


class Robot:
    def __init__(self, grid):
        # Note that we assume the robot starts always facing the top.
        # This value will never change, but it will not affect us as the robot uses a more fine-tuned internal
        # angle tracker.
        self.grid = grid

        self.simple_hamiltonian = tuple()

        self.pos = RobotPosition(75,
                                 75,
                                 Direction.TOP,
                                 90)
                                 
        self._start_copy = self.pos.copy()

        #self.brain = Brain(self, grid)

        self.__image = pygame.transform.scale(pygame.image.load("assets/robot.png"),
                                              (150, 150))

        self.path_hist = []  # Stores the history of the path taken by the robot.

        self.__current_command = 0  # Index of the current command being executed.
        self.printed = False  # Never printed total time before.

    def get_current_pos(self):
        return self.pos

    def convert_all_commands(self):
        """
        Convert the list of command objects to corresponding list of messages.
        """
        print("Converting commands to string...", end="")
        string_commands = [command.convert_to_message() for command in self.brain.commands]
        print("Done!")
        return string_commands

    def convert_commands(self):
        """
        Convert the command object to list of messages.
        """
        print("Converting commands to string...", end="")
        string_commands = [command.convert_to_message() for command in self.brain.commands]
        print("Done!")
        print("-" * 70)
        return string_commands
        
    #def turn(self, d_angle, rev):
      

    #def straight(self, dist):
        
    
    def draw_simple_hamiltonian_path(self, screen):
        prev = self._start_copy.xy_pygame()
        for obs in self.simple_hamiltonian:
            target = obs.get_robot_target_pos().xy_pygame()
            pygame.draw.line(screen, BLUE, prev, target)
            prev = target
   
    def draw_self(self, screen):
        # The arrow to represent the direction of the robot.
        rot_image = pygame.transform.rotate(self.__image, -(90 - self.pos.angle))
        rect = rot_image.get_rect()
        rect.center = self.pos.xy_pygame()
        screen.blit(rot_image, rect)

    def draw_historic_path(self, screen):
        for dot in self.path_hist:
            pygame.draw.circle(screen, BLACK, dot, 3)

    def draw(self, screen):
        # Draw the robot itself.
        self.draw_self(screen)
        # Draw the simple hamiltonian path found by the robot.
        self.draw_simple_hamiltonian_path(screen)
        # Draw the path sketched by the robot
        #self.draw_historic_path(screen)

    def compute_simple_hamiltonian_path(self) -> Tuple[Obstacle]:
        """
        Get the Hamiltonian Path to all points with the best possible effort.
        This is a simple calculation where we assume that we travel directly to the next obstacle.
        """
        # Generate all possible permutations for the image obstacles
        perms = list(itertools.permutations(self.grid.obstacles))

        index_list = []

        # Get the path that has the least distance travelled.
        def calc_distance(path):
            # Create all target points, including the start.
            targets = [self.pos.xy_pygame()]

            for obstacle in path:
                targets.append(obstacle.pos.xy_pygame())

            dist = 0
            for i in range(len(targets) - 1):
                dist += math.sqrt(((targets[i][0] - targets[i + 1][0]) ** 2) +
                                  ((targets[i][1] - targets[i + 1][1]) ** 2))
            return dist

        simple = min(perms, key=calc_distance)
        
        print("Found a simple hamiltonian path:")
        for ob in simple:
            index_list.append(ob.getIndex())
            print(f"{ob}")
        return simple, index_list
    
    #def compress_paths(self):
       

    def plan_path(self):
        print("-" * 70)
        print("Starting path computation...")
        self.simple_hamiltonian, index_list = self.compute_simple_hamiltonian_path()

        return index_list #place it here first
    """    
        
        curr = self.robot.pos.copy()  # We use a copy rather than get a reference.
        for obstacle in self.simple_hamiltonian:
            target = obstacle.get_robot_target_pos()
            print("-" * 70)
            print(f"Planning {curr} to {target}")
            res = ModifiedAStar(self.grid, self, curr, target).start_astar()
            if res is None:
                print(f"No path found from {curr} to {obstacle}")
            else:
                print("Path found.")
                curr = res
                self.commands.append(ScanCommand(ROBOT_SCAN_TIME, obstacle.index))

        self.compress_paths()
        print("-" * 70)
        
        #return index_list
        

    def update(self):
        # Store historic path
        if len(self.path_hist) == 0 or self.pos.xy_pygame() != self.path_hist[-1]:
            # Only add a new point history if there is none, and it is different from previous history.
            self.path_hist.append(self.pos.xy_pygame())

        # If no more commands to execute, then return.
        if self.__current_command >= len(self.brain.commands):
            return

        # Check current command has non-null ticks.
        # Needed to check commands that have 0 tick execution time.
        if self.brain.commands[self.__current_command].total_ticks == 0:
            self.__current_command += 1
            if self.__current_command >= len(self.brain.commands):
                return

        # If not, the first command in the list is always the command to execute.
        command: Command = self.brain.commands[self.__current_command]
        command.process_one_tick(self)
        # If there are no more ticks to do, then we can assume that we have
        # successfully completed this command, and so we can remove it.
        # The next time this method is run, then we will process the next command in the list.
        if command.ticks <= 0:
            print(f"Finished processing {command}, {self.pos}")
            self.__current_command += 1
            if self.__current_command == len(self.brain.commands) and not self.printed:
                total_time = 0
                for command in self.brain.commands:
                    total_time += command.time
                    total_time = round(total_time)
                # Calculate time for all commands
                # Then print it out.
                print(f"All commands took {datetime.timedelta(seconds=total_time)}")
                self.printed = True


"""   