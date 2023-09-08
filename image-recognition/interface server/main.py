from flask import Flask, request, jsonify
from flask_cors import CORS
from model import *

app = Flask(__name__)
CORS(app)

# model = load_model()
model = None

@app.route('/status', methods = ['GET'])
def get_status():
    # check if the server is running
    return jsonify({"result": "ok"})

@app.route('/image', methods=['POST'])
def image_rec():
    file = request.files['file']
    filename = file.filename
    file.save(os.path.join('uploads', filename))

    # TODO: rec_image function in model.py
    image_id = rec_image(filename, model)

    result = {
        "image_id": image_id
    }

    return jsonify(result)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5007, debug=True)