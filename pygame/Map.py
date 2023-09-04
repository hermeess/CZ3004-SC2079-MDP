from enum import Enum #check
import pygame
from collections import deque
from typing import List
import math

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

class Position: 
   def __init__(self, x, y, direction: Direction = None):
     """
     x and y coordinates are with respect to the grid
     Note that they should be scaled properly already (not sure what this means)
     Angle should be in DEGREES
     
     """
     self.x = x 
     self.y = y 
     self.direction = direction 
     
   def __str__(self):
     return f"Position({(self.x // SCALING_FACTOR)}, {self.y // SCALING_FACTOR},  "f"angle={self.direction})"
   __repr__ = __str__

   def xy(self):
        """
        Return the true x, y coordinates of the current Position.
        """
        return self.x, self.y
   """
   def descaled_xy(self): 
        x_descaled = self.x / 5
        y_descaled = self.y / 5
        return x_descaled, y_descaled
   """ #this function is commented out for now because it is never called 
   def xy_dir(self):
        return *self.xy(), self.direction

   def xy_pygame(self):
        """
        Return the x, y coordinates in terms of Pygame coordinates. Useful for drawing on screen.
        """
        return self.x, GRID_LENGTH - self.y

   def copy(self):
        """
        Create a new copy of this Position.
        """
        return Position(self.x, self.y, self.direction)
     
class RobotPosition(Position):
    def __init__(self, x, y, direction: Direction = None, angle=None):
        super().__init__(x, y, direction)
        self.angle = angle
        if direction is not None:
            self.angle = direction.value

    def __str__(self):
        return f"RobotPosition({super().__str__()}, angle={self.angle})"

    __repr__ = __str__

    def copy(self):
        return RobotPosition(self.x, self.y, self.direction, self.angle)

    def get_pos(self):
        return self.x, self.y, self.direction
    
class Node: 
  
    def __init__(self, x, y, occupied, direction=None):
        """
        x and y coordinates are in terms of the grid.
        """
        self.pos = Position(x, y, direction)
        self.occupied = occupied
        self.x = x
        self.y = y

    def __str__(self):
        return f"Node({self.pos})"

    __repr__ = __str__

    def __eq__(self, other):
        return self.pos.xy_dir() == other.pos.xy_dir()

    def __hash__(self):
        return hash(self.pos.xy_dir())

    def xy_descaled(self):
        x_descaled = self.x / 5
        y_descaled = self.y / 5
        return x_descaled, y_descaled
        
    def copy(self):
        """
        Return a copy of this node.
        """
        return Node(self.pos.x, self.pos.y, self.occupied, self.pos.direction)

    def draw_self(self, screen):
        if self.occupied:  # If current node is not permissible to the robot
            rect = pygame.Rect(0, 0, GRID_CELL_LENGTH, GRID_CELL_LENGTH)
            rect.center = self.pos.xy_pygame()
            pygame.draw.rect(screen, DARK_GREY , rect)

    def draw_boundary(self, screen):
        x_pygame, y_pygame = self.pos.xy_pygame()

        left = x_pygame - GRID_CELL_LENGTH // 2
        right = x_pygame + GRID_CELL_LENGTH // 2
        top = y_pygame - GRID_CELL_LENGTH // 2
        bottom = y_pygame + GRID_CELL_LENGTH // 2

        # Draw
        pygame.draw.line(screen, GREY, (left, top), (left, bottom))  # Left border
        pygame.draw.line(screen, GREY, (left, top), (right, top))  # Top border
        pygame.draw.line(screen, GREY, (right, top), (right, bottom))  # Right border
        pygame.draw.line(screen, GREY, (left, bottom), (right, bottom))  # Bottom border

    def draw(self, screen):
        # Draw self
        self.draw_self(screen)
        # Draw node border
        self.draw_boundary(screen)

class Obstacle:
    def __init__(self, x, y, direction, index):
        """
        x -> x-coordinate of the obstacle.
        y -> y-coordinate of the obstacle.
        Note x, y coordinates should not be scaled.
        direction -> Which direction the image is facing. If image is on the right side of the obstacle, RIGHT.
        """
        # Check if the coordinates are multiples of 10 with offset 5. If they are not, then they are invalid
        # obstacle coordinates.
        # This is from the assumption that all obstacles are placed centered in each grid.
        if (x - 5) % 10 != 0 or (y - 5) % 10 != 0:
            raise AssertionError("Obstacle center coordinates must be multiples of 10 with offset 5!")

        # Translate given coordinates to be in PyGame coordinates.
        self.pos = Position(x * SCALING_FACTOR, y * SCALING_FACTOR, direction)
        # Arrow to draw at the target coordinate.
        self.target_image = pygame.transform.scale(pygame.image.load("assets/jasontheween.png"), (50, 50))

        self.index = index
        
    def getIndex(self):
        return self.index

    def __str__(self):
        return f"Obstacle({self.pos})"

    __repr__ = __str__

    def check_within_boundary(self, x, y):
        """
        Checks whether a given x-y coordinate is within the safety boundary of this obstacle.
        """
        if ((self.pos.x - (OBSTACLE_SAFETY_WIDTH - 20)) < x < (self.pos.x + OBSTACLE_SAFETY_WIDTH)) and \
            ((self.pos.y - (OBSTACLE_SAFETY_WIDTH - 20)) < y < (self.pos.y + OBSTACLE_SAFETY_WIDTH)):
            return True
        return False

    def get_boundary_points(self):
        """
        Get points at the corner of the virtual obstacle for this image.

        Useful for checking if a point is within the boundary of this obstacle.
        """
        upper = self.pos.y + OBSTACLE_SAFETY_WIDTH 
        lower = self.pos.y - OBSTACLE_SAFETY_WIDTH 
        left = self.pos.x - OBSTACLE_SAFETY_WIDTH 
        right = self.pos.x + OBSTACLE_SAFETY_WIDTH

        return [
            # Note that in this case, the direction does not matter.
            Position(left, lower),  # Bottom left.
            Position(right, lower),  # Bottom right.
            Position(left, upper),  # Upper left.
            Position(right, upper)  # Upper right.
        ]

    def get_robot_target_pos(self):
        """
        Returns the point that the robot should target for, including the target orientation.

        Note that the target orientation is now with respect to the robot. If the robot needs to face right, then
        we use 0 degrees.

        We can store this information within a Position object.

        The object will also store the angle that the robot should face.
        """
        if self.pos.direction.value == 90: #TOP
            return RobotPosition(self.pos.x, self.pos.y + OBSTACLE_SAFETY_WIDTH + OBSTACLE_LENGTH + 50, Direction.BOTTOM)
        elif self.pos.direction.value == -90: #BOTTOM
            return RobotPosition(self.pos.x, self.pos.y - OBSTACLE_SAFETY_WIDTH - OBSTACLE_LENGTH - 50, Direction.TOP)
        elif self.pos.direction.value == 180: #LEFT
            return RobotPosition(self.pos.x - OBSTACLE_SAFETY_WIDTH - OBSTACLE_LENGTH - 50, self.pos.y, Direction.RIGHT)
        else: #RIGHT
            return RobotPosition(self.pos.x + OBSTACLE_SAFETY_WIDTH + OBSTACLE_LENGTH + 50, self.pos.y, Direction.LEFT)

    def draw_self(self, screen):
        # Draw the obstacle onto the grid.
        # We need to translate the obstacle's center into that with respect to PyGame
        # Get the coordinates of the grid's bottom left-hand corner.
        rect = pygame.Rect(0, 0, OBSTACLE_LENGTH, OBSTACLE_LENGTH)
        rect.center = self.pos.xy_pygame()
        pygame.draw.rect(screen, BLACK, rect)

        # Draw the direction of the picture
        rect.width = OBSTACLE_LENGTH / 2
        rect.height = OBSTACLE_LENGTH / 2
        rect.center = self.pos.xy_pygame()
    
        if self.pos.direction.value == 90: #TOP
            rect.centery -= OBSTACLE_LENGTH / 4
        elif self.pos.direction.value == -90: #BOTTOM
            rect.centery += OBSTACLE_LENGTH / 4
        elif self.pos.direction.value == 180: #LEFT
            rect.centerx -= OBSTACLE_LENGTH / 4 
        elif self.pos.direction.value == 0: #RIGHT
            rect.centerx += OBSTACLE_LENGTH / 4

        # Draw the picture place
        pygame.draw.rect(screen, GREEN, rect)

    def draw_virtual_boundary(self, screen):
        # Get the boundary points
        points = self.get_boundary_points()

        # Draw left border
        pygame.draw.line(screen, RED, points[0].xy_pygame(), points[2].xy_pygame())
        # Draw right border
        pygame.draw.line(screen, RED, points[1].xy_pygame(), points[3].xy_pygame())
        # Draw upper border
        pygame.draw.line(screen, RED, points[2].xy_pygame(), points[3].xy_pygame())
        # Draw lower border
        pygame.draw.line(screen, RED, points[0].xy_pygame(), points[1].xy_pygame())

    def draw_robot_target(self, screen):
        target = self.get_robot_target_pos()

        rot_image = self.target_image
        angle = 0

        if target.direction == Direction.BOTTOM:
            angle = 180
        elif target.direction == Direction.LEFT:
            angle = 90
        elif target.direction == Direction.RIGHT:
            angle = -90

        rot_image = pygame.transform.rotate(rot_image, angle)
        rect = rot_image.get_rect()
        rect.center = target.xy_pygame()
        screen.blit(rot_image, rect)

    def draw(self, screen):
        # Draw the obstacle itself.
        self.draw_self(screen)
        # Draw the obstacle's boundary.
        self.draw_virtual_boundary(screen)
        # Draw the target for this obstacle.
        self.draw_robot_target(screen)

class Grid:
    def __init__(self, obstacles: List[Obstacle]):
        self.obstacles = obstacles
        self.nodes = self.generate_nodes()

    def generate_nodes(self):
        """
        Generate the nodes for this grid.
        """
        nodes = deque()
        for i in range(GRID_NUM_GRIDS):
            row = deque()
            for j in range(GRID_NUM_GRIDS):
                x, y = (GRID_CELL_LENGTH / 2 + GRID_CELL_LENGTH * j), \
                       (GRID_CELL_LENGTH / 2 + GRID_CELL_LENGTH * i)
                new_node = Node(x, y, not self.check_valid_position(Position(x, y)))
                row.append(new_node)
            nodes.appendleft(row)
        return nodes

    def get_coordinate_node(self, x, y):
        """
        Get the corresponding Node object that contains specified x, y coordinates.

        Note that the x-y coordinates are in terms of the grid, and must be scaled properly.
        """
        col_num = math.floor(x / GRID_CELL_LENGTH)
        row_num = GRID_NUM_GRIDS - math.floor(y / GRID_CELL_LENGTH) - 1
        try:
            return self.nodes[row_num][col_num]
        except IndexError:
            return None

    def copy(self):
        """
        Return a copy of the grid.
        """
        nodes = []
        for row in self.nodes:
            new_row = []
            for col in row:
                new_row.append(col.copy())
            nodes.append(new_row)
        new_grid = Grid(self.obstacles)
        new_grid.nodes = nodes
        return new_grid

    def delete_obstacle(self):
        self.obstacles.pop(0)
        return self.obstacles

    def check_valid_position(self, pos: Position):
        """
        Check if a current position can be here.
        """
        # Check if position is inside any obstacle.
        if any(obstacle.check_within_boundary(*pos.xy()) for obstacle in self.obstacles):
            return False

        # Check if position too close to the border.
        # NOTE: We allow the robot to overextend the border a little!
        # We do this by setting the limit to be GRID_CELL_LENGTH rather than ROBOT_SAFETY_DISTANCE
        if ((pos.y < 25 or
            pos.y > GRID_LENGTH)) or \
                ((pos.x < 25 or
                 pos.x > GRID_LENGTH)):
            return False
        return True

    @classmethod
    def draw_arena_borders(cls, screen):
        """
        Draw the arena borders.
        """
        # Draw upper border
        pygame.draw.line(screen, BLACK, (0, 0), (GRID_LENGTH, 0))
        # Draw lower border
        pygame.draw.line(screen, BLACK, (0, GRID_LENGTH), (GRID_LENGTH, GRID_LENGTH))
        # Draw left border
        pygame.draw.line(screen, BLACK, (0, 0), (0, GRID_LENGTH))
        # Draw right border
        pygame.draw.line(screen, BLACK, (GRID_LENGTH, 0), (GRID_LENGTH, GRID_LENGTH))

    def draw_obstacles(self, screen):
        for ob in self.obstacles:
            ob.draw(screen)

    def draw_nodes(self, screen):
        for row in self.nodes:
            for col in row:
                col.draw(screen)

    def draw(self, screen):
        # Draw nodes
        self.draw_nodes(screen)
        # Draw arena borders
        self.draw_arena_borders(screen)
        # Draw obstacles
        self.draw_obstacles(screen)