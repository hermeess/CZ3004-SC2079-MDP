import pygame
import time
import json
from typing import List #check 
from Map import *
from simulator import AlgoSimulator, AlgoMinimal
from settings import *



start_img = pygame.image.load("assets/jasontheween.png").convert_alpha()
exit_img = pygame.image.load("assets/jasontheween.png").convert_alpha()
reset_img = pygame.image.load("assets/jasontheween.png").convert_alpha()


old_obstacles = [{"x":1,"y":18,"direction":-90,"obs_id":0},
                 {"x":16,"y":10,"direction":90,"obs_id":1},
                 {"x":12,"y":3,"direction":90,"obs_id":2},
                 {"x":18,"y":18,"direction":-90,"obs_id":3},
                 {"x":2,"y":8,"direction":-90,"obs_id":4},
                 {"x":5,"y":12,"direction":-90,"obs_id":5}]

def parse_obstacle_data_cur(data) -> List[Obstacle]:
    obs = []
    lst3 = []
    lst = []
    i = 0

    for obj in data:
        lst.append(obj)

    for i in lst:
        i["x"] *= 10
        i["x"] += 5
        i["y"] *= 10
        i["y"] += 5
        #i["obs_id"] -= 1

    a = [list(row) for row in zip(*[m.values() for m in lst])]

    for i in range(len(a[0])):
        lst2 = [item[i] for item in a]
        lst3.append(lst2)
        i+=1
        
    for obstacle_params in lst3:
        obs.append(Obstacle(obstacle_params[0],
                            obstacle_params[1],
                            Direction(obstacle_params[2]),
                            obstacle_params[3]))

    # [[x, y, orient, index], [x, y, orient, index]]
    return obs 
     

def main(simulator):
   # simulator: Pass in True to show simulator screen
    
    index = 0
    i = 0
    reverse = "BW020"
    scan = "SNAPX_C"
    forward = "FW020"
    reverseSecond = "BW010"
    forwardSecond = "FW030"
    
    """
    obst_list = [{"x":1,"y":18,"direction":-90,"obs_id":0},
                 {"x":16,"y":10,"direction":90,"obs_id":1},
                 {"x":12,"y":3,"direction":90,"obs_id":2},
                 {"x":18,"y":18,"direction":-90,"obs_id":3},
                 {"x":2,"y":8,"direction":-90,"obs_id":4},
                 {"x":5,"y":12,"direction":-90,"obs_id":5}]
    """
    
    image_ids = ["11", "12", "13", "14", "15", "16", "17", "18", "19", "20", 
                 "21", "22", "23", "24", "25", "26", "27", "28", "29", "30",
                 "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40"]

    # Create a client to send and receive information from the RPi
    #client = Client("192.168.13.1", 3004)  # 10.27.146 139 | 192.168.13.1
    #client.connect()
    
  
   
    # ANDROID send obstacle positions to ALGO
    print("===========================Receive Obstacles Data===========================")
    print("Waiting to receive obstacle data from ANDROID...")

    """"
    obstacle_data = client.receive()
    data2 = json.loads(obstacle_data)
    obst_list.append(data2)

    while obstacle_data != "PC;START":
        #obstacle_data = client.receive()
        if obstacle_data == "PC;START":
            break
        data = json.loads(obstacle_data)
        print(data)
        obst_list.append(data)
        i+=1
    
    obst_list.pop()
    print("Received all obstacles data from ANDROID.")
    print(f"Obstacles data: {obst_list}")
    """



   
    obstacles = parse_obstacle_data_cur(obst_list)
    

   
    if simulator == True:
        app = AlgoSimulator(obstacles)
        app.init()
        app.execute()
    else:
        app = AlgoMinimal(obstacles)
        index_list = app.execute()
    commands = app.robot.convert_commands()
    commands.append("FIN")
    list_of_coor = app.robot.path_hist
    
    
    obj = {}
    obj['data'] = {
        'commands': commands,
        'path': list_of_coor
    }
    jsonObj = json.dumps(obj)
   

    roboPosCoor = {
        "x": 15,
        "y": 4,
        "direction" : "N"
    }
    
    
  


if __name__ == '__main__':
    main(True)
"""
obstacles = parse_obstacle_data_cur(old_obstacles)
app = AlgoSimulator(obstacles)
app.init()
app.execute()

"""

