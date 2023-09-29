import itertools
import math
from collections import deque
from typing import Tuple
from Map import Obstacle
from Robot.commands import *
from settings import *
from Robot.path_algo import ModifiedAStar


class Brain:
    def __init__(self, robot, grid):
        self.robot = robot
        self.grid = grid

        # Compute the simple Hamiltonian path for all obstacles
        self.simple_hamiltonian = tuple()

        # Create all the commands required to finish the course.
        self.commands = deque()


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
            targets = [self.robot.pos.xy_pygame()]

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

    def compress_paths(self):
        """
        Compress similar commands into one command.

        Helps to reduce the number of commands.
        """
        print("Compressing commands... ", end="")
        index = 0
        new_commands = deque()
        while index < len(self.commands):
            command = self.commands[index]
            if isinstance(command, StraightCommand):
                new_length = 0
                while index < len(self.commands) and isinstance(self.commands[index], StraightCommand):
                    new_length += self.commands[index].dist
                    index += 1
                command = StraightCommand(new_length)
                new_commands.append(command)
            else:
                new_commands.append(command)
                index += 1
        self.commands = new_commands
        print("Done!")

    def plan_path(self):
        print("-" * 70)
        print("Starting path computation...")
        self.simple_hamiltonian, index_list = self.compute_simple_hamiltonian_path()

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
        return index_list

    # def plan_bullseye(self):
    #     print("-" * 40)
    #     print("STARTING PATH COMPUTATION AFTER ENCOUNTERING BULLSEYE...")
    #     self.simple_hamiltonian = self.compute_simple_hamiltonian_path()
    #     print()

    #     curr = self.robot.pos.copy()  # We use a copy rather than get a reference.
    #     for obstacle in self.simple_hamiltonian:
    #         target = obstacle.get_robot_target_pos()
    #         print(f"Planning {curr} to {target}")
    #         res = ModifiedAStar(self.grid, self, curr, target).start_astar()
    #         if res is None:
    #             print(f"\tNo path found from {curr} to {obstacle}")
    #         else:
    #             print("\tPath found.")
    #             curr = res
    #             self.commands.append(ScanCommand(ROBOT_SCAN_TIME, obstacle.index))

    #     self.compress_paths()
    #     print("-" * 40)