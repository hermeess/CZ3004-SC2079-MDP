from flask import Flask, request, jsonify
from flask_cors import CORS
from model import *
import importlib.util
import sys

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
    
    # #week 8
    rec_result = rec_image(filename, model, signal)
    
    #week 9
    # rec_result = rec_image_week9(filename, model, signal)

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
async def cal_path():
    spec = importlib.util.spec_from_file_location("module.name", "/Users/jiaxi/Desktop/MDP/github/CZ3004-SC2079-MDP/pygame/main.py ")
    foo = importlib.util.module_from_spec(spec)
    sys.modules["module.name"] = foo
    spec.loader.exec_module(foo)
    await foo.main()
    return 'NA'

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)