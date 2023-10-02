from flask import Flask, request, jsonify
from flask_cors import CORS
from model import *
from algorithm.mainAlgo import *

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
    print('request: ', request.get_json())
    obst_list = request.get_json()['obstacles']

    result = mainAlgoFunc(False, obst_list)
    print("result from algo:", result)

    return jsonify(result)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5007, debug=True)