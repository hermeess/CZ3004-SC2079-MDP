from PIL import Image
from ultralytics import YOLO
import os
import cv2
import numpy as np
import glob
import math

def load_model():
    # model = torch.hub.load('./', 'custom', path='../weights/Week8_senior.pt', source='local')
    
    # week 8
    model = YOLO('../weights/week8_final.pt')
    
    #week 9
    # model = YOLO('../image-recognition/weights/week9_2.04.pt')
    
    return model

def draw_bbox(img, image_name, x1, y1, x2, y2, image_id, color=(255,255,255), text_color=(0,0,0)):
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
    cv2.imwrite(f"image_results/raw_image_{image_name}", img)

    # draw bounding box
    img = cv2.rectangle(img, (x1, y1), (x2, y2), (36,255,12), 2)
    # print the text
    img = cv2.rectangle(img, (x2 + 100, y1), (x2 + 450, y1 + 200), color, -1)
    img = cv2.putText(img, id_to_name[int(image_id)], (x2 + 120, y1 + 80), cv2.FONT_HERSHEY_SIMPLEX, 1.5, text_color, 3)
    img = cv2.putText(img, 'Image id='+image_id, (x2 + 120, y1 + 150), cv2.FONT_HERSHEY_SIMPLEX, 1.5, text_color, 3)
    # save annotated image
    cv2.imwrite(f"image_results/annotated_image_{image_name}", img)


def rec_image(image, model, signal):
    
    # load image
    img = Image.open(os.path.join('uploads', image))
    results = model.predict(img)
    result = results[0]

    rec_result = []

    if len(result.boxes) == 0:
        return {
            'image_id': 'NA'
        }

    print("-----Recognize results-----")
    for box in result.boxes:
        image_id = result.names[box.cls[0].item()]
        bbox = box.xyxy[0].tolist()
        bbox_area = (bbox[2] - bbox[0]) * (bbox[3] - bbox[1])
        confidence = round(box.conf[0].item(), 3)

        print("Image ID:", image_id)
        print("Bounding box coordinates:", bbox)
        print("Bounding box area: ", bbox_area)
        print("Probability:", confidence)
        
        rec_result.append({
            "image_id": image_id,
            "bbox": bbox,
            "bbox_area": bbox_area,
            "prob": confidence
        })


    rec_result.sort(key=lambda x: x['bbox_area'], reverse=True)
    filtered_rec_result = [re for re in rec_result if re["image_id"] != '99']

    final_rec = {}

    if len(filtered_rec_result) > 1:

        # filter the result list by bounding box area
        shortlisted_rec_result = []
        current_area = filtered_rec_result[0]['bbox_area']

        for i in range(len(filtered_rec_result)):
            if (filtered_rec_result[i]['bbox_area'] >= current_area * 0.8) or (filtered_rec_result[i]['image_id'] == '11' and filtered_rec_result[i][bbox_area] >= current_area * 0.6):
                shortlisted_rec_result.append(filtered_rec_result[i])

        # if multiple results remian after filtering by bounding box area
        if len(shortlisted_rec_result) > 1:
            # use signal to filter
            shortlisted_rec_result.sort(key=lambda x:x['bbox'][0])

            if signal == 'L':
                final_rec = shortlisted_rec_result[0]

            elif signal == 'R':
                final_rec = shortlisted_rec_result[-1]

            else: # signal == 'C'
                final_rec = shortlisted_rec_result[len(shortlisted_rec_result)//2]
        else:
            final_rec = shortlisted_rec_result[0]

    else: # only one result in list
        final_rec = filtered_rec_result[0]


    final_bbox = final_rec['bbox']
    final_id = final_rec['image_id']

    draw_bbox(np.array(img),image, final_bbox[0], final_bbox[1], final_bbox[2], final_bbox[3], final_id)

    return final_rec

def rec_image_week9(image, model, signal):
    
    # load image
    img = Image.open(os.path.join('uploads', image))
    results = model.predict(img)
    result = results[0]

    rec_result = []

    if len(result.boxes) == 0:
        return {
            'image_id': 'NA'
        }

    print("-----Recognize results-----")
    for box in result.boxes:
        image_id = result.names[box.cls[0].item()][2:]
        # bbox = box.xyxy[0].tolist()
        # bbox_area = (bbox[2] - bbox[0]) * (bbox[3] - bbox[1])
        confidence = round(box.conf[0].item(), 3)

        print("Image ID:", image_id)
        # print("Bounding box coordinates:", bbox)
        # print("Bounding box area: ", bbox_area)
        print("Probability:", confidence)
        
        rec_result.append({
            "image_id": image_id,
            # "bbox": bbox,
            # "bbox_area": bbox_area,
            "prob": confidence
        })

    rec_result.sort(key=lambda x: x['prob'], reverse=True) # sort by confidence

    filtered_rec_result = [re for re in rec_result if re["image_id"] != '99']
    # by right there should be only one result after filtering out bulleyes

    return filtered_rec_result[0]

def combine_image():

    folder_path = 'image_results/'

    # Use glob to find files with names starting with "annotated_image_"
    annotated_images = glob.glob(os.path.join(folder_path, 'annotated_image_*'))

    # open all images
    images = [Image.open(x) for x in annotated_images]

    # number of images in a row
    num = math.ceil(len(annotated_images) / 2)

    width, height = zip(*(i.size for i in images))
    final_width = max(width) * num + 50 * num + 50
    final_height = max(height) * 2 + 50 * 3
    combinedImg = Image.new('RGB', (final_width, final_height))

    x_offset = 50
    y_offset = 50
    x_num = 0 # how many images in current row

    for img in images:
        combinedImg.paste(img, (x_offset, y_offset))
        x_offset = x_offset + img.size[0] + 50
        x_num += 1
        if x_num == num:
            x_num = 0
            x_offset = 50
            y_offset = 50 + max(height) + 50

    combinedImg.save(os.path.join(folder_path, 'combinedImg.jpeg'))

    return combinedImg
