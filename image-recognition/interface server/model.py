from PIL import Image
from ultralytics import YOLO
import torch
import os
import cv2
import numpy as np

def load_model():
    # model = torch.hub.load('./', 'custom', path='week8.pt', source='local')
    model = YOLO('week8.pt')
    return model

def draw_bbox(img, x1, y1, x2, y2, image_id, color=(255,255,255), text_color=(0,0,0)):
    # convert coordinates to int
    x1 = int(x1)
    x2 = int(x2)
    y1 = int(y1)
    y2 = int(y2)

    id_to_name = {
        11: 'Number 1',
        12: 'Number 2',
        13: 'Number 3', 
        14: 'Number 4',
        15: 'Number 5',
        16: 'Number 6',
        17: 'Number 7',
        18: 'Number 8',
        19: 'Number 9',
        20: 'Alphabet A',
        21: 'Alphabet B',
        22: 'Alphabet C',
        23: 'Alphabet D',
        24: 'Alphabet E',
        25: 'Alphabet F',
        26: 'Alphabet G',
        27: 'Alphabet H',
        28: 'Alphabet S',
        29: 'Alphabet T',
        30: 'Alphabet U',
        31: 'Alphabet V',
        32: 'Alphabet W',
        33: 'Alphabet X',
        34: 'Alphabet Y',
        35: 'Alphabet Z',
        36: 'Up arrow',
        37: 'Down arrow',
        38: 'Right arrow',
        39: 'Left arrow',
        40: 'Stop',
        99: 'Bulleye'
    }

    if not os.path.exists('image_results'):
        os.makedirs('image_results')

    # save raw image
    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    cv2.imwrite(f"image_results/raw_image_{image_id}.jpg", img)

    # draw bounding box
    img = cv2.rectangle(img, (x1, y1), (x2, y2), (36,255,12), 2)
    # print the text
    img = cv2.rectangle(img, (x2 + 100, y1), (x2 + 450, y1 + 200), color, -1)
    img = cv2.putText(img, id_to_name[int(image_id)], (x2 + 120, y1 + 80), cv2.FONT_HERSHEY_SIMPLEX, 1.5, text_color, 3)
    img = cv2.putText(img, 'Image id='+image_id, (x2 + 120, y1 + 150), cv2.FONT_HERSHEY_SIMPLEX, 1.5, text_color, 3)
    # save annotated image
    cv2.imwrite(f"image_results/annotated_image_{image_id}.jpg", img)


def rec_image(image, model):
    
    # load image
    img = Image.open(os.path.join('uploads', image))
    results = model.predict(img)
    result = results[0]

    rec_result = 'NA'

    print("-----Recognize results-----")

    for box in result.boxes:
        image_id = result.names[box.cls[0].item()]
        bbox = box.xyxy[0].tolist()
        confidence = round(box.conf[0].item(), 3)
        print("Image ID:", image_id)
        print("Bounding box coordinates:", bbox)
        print("Probability:", confidence)
        draw_bbox(np.array(img), bbox[0], bbox[1], bbox[2], bbox[3], image_id)
        rec_result = image_id

    return rec_result
        