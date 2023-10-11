from flask import Flask, request, jsonify
from flask_cors import CORS
from model import *
#from mainAlgo import *
from algo.algo import MazeSolver 
from helper import command_generator
import time

app = Flask(__name__)
CORS(app)

model = load_model()
# model = None

UPLOAD_FOLDER = 'uploads'

@app.route('/status', methods = ['GET'])
def get_status():
    # check if the server is running
    return jsonify({"result": "ok"})

@app.route('/image', methods=['POST'])
def image_rec():
    '''
     Filename format: "<timestamp>_<obstacle_id>_<signal>.jpg"
    '''
    file = request.files['file']
    filename = file.filename

    # Split the string by underscore
    parts = filename.split("_")

    # Extract the values
    obstacle_id = parts[1]  # <obstacle_id>
    signal = parts[2].split(".")[0]  # <signal>

    filename = parts[0] + '.jpg' # only leave the timestamp for filename

    if not os.path.exists(UPLOAD_FOLDER):
        os.makedirs(UPLOAD_FOLDER)

    file.save(os.path.join('uploads', filename))

    # TODO: rec_image function in model.py
    rec_result = rec_image(filename, model, signal)

    result = {
        "image_id": str(rec_result['image_id']),
        "obstacle_id": str(obstacle_id)
    }

    return jsonify(result)

@app.route('/stitch', methods=['GET'])
def combine():
    image = combine_image()
    image.show()
    return jsonify({"result": "ok"})

@app.route('/path', methods=['POST'])
def cal_path():
    """
    This is the main endpoint for the path finding algorithm
    :return: a json object with a key "data" and value a dictionary with keys "distance", "path", and "commands"
    """
    # Get the json data from the request
    print("Received algo request")
    content = request.json

    # Get the obstacles, big_turn, retrying, robot_x, robot_y, and robot_direction from the json data
    obstacles = content['obstacles']
    # big_turn = int(content['big_turn'])
    retrying = content['retrying']
    robot_x, robot_y = content['robot_x'], content['robot_y']
    robot_direction = int(content['robot_dir'])

    # Initialize MazeSolver object with robot size of 20x20, bottom left corner of robot at (1,1), facing north, and whether to use a big turn or not.
    maze_solver = MazeSolver(20, 20, robot_x, robot_y, robot_direction, big_turn=1)
    obstacle_dict = {}

    # Add each obstacle into the MazeSolver. Each obstacle is defined by its x,y positions, its direction, and its id
    for ob in obstacles:
        if (ob['d'] == 0):
            ob['d'] = 2
        elif (ob['d'] == -90):
            ob['d'] = 4
        elif (ob['d'] == 90):
            ob['d'] = 0
        elif (ob['d'] == 180):
            ob['d'] = 6
        maze_solver.add_obstacle(ob['x'], ob['y'], ob['d'], ob['id'])
        obstacle_dict[str(ob['id'])] = {"x": ob['x'], "y": ob['y']}

    start = time.time()
    # Get shortest path
    optimal_path, distance = maze_solver.get_optimal_order_dp(retrying=retrying)
    print(f"Time taken to find shortest path using A* search: {time.time() - start}s")
    print(f"Distance to travel: {distance} units")
    
    # Based on the shortest path, generate commands for the robot
    commands = command_generator(optimal_path, obstacles)

    # Get the starting location and add it to path_results
    path_results = [optimal_path[0].get_dict()]
    # Process each command individually and append the location the robot should be after executing that command to path_results
    i = 0

    return_command = []
    for command in commands:
        if command.startswith("SNAP"):
            obstacle_id = command[4]
            obstacle = obstacle_dict[obstacle_id]
            x_diff = abs (path_results[-1]['x'] - obstacle['x'])
            y_diff = abs (path_results[-1]['y'] - obstacle['y'])

            dist_before_obs = max(x_diff, y_diff)
            return_command.append("AD{:03d}".format(dist_before_obs * 10 - 20))
            return_command.append(command)
            continue
        if command.startswith("FIN"):
            return_command.append(command)
            continue
        elif command.startswith("FW") or command.startswith("FS"):
            i += int(command[2:]) // 10
            return_command.append(command)
        elif command.startswith("BW") or command.startswith("BS"):
            i += int(command[2:]) // 10
            return_command.append(command)
        else:
            i += 1
            return_command.append(command)
        path_results.append(optimal_path[i].get_dict())
    
    print (return_command)
    
    return jsonify({
        "data": {
            'distance': distance,
            'path': path_results,
            'commands': return_command
        },
        "error": None
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5007, debug=True)