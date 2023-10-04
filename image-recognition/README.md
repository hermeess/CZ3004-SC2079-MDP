# CZ3004-SC2079-MDP-Image Recognition

This is image recognition part for group 24 MDP.

## Table of Content

- [Information](#information)
- [Usage](#usage)

<br>

## Information

## *Interface server*

### API Endpoint

`[GET] /status` <br>
health checking

`[POST] /image` <br>
main image recognition function <br>
require an image file in the request body <br>
Sample request (generated by Postman):
```
response = requests.request(
  "POST", 
  url='http://localhost:5007/image', 
  headers={}, 
  data={'signal': 'C'}, 
  files=image_for_recognition
)
```

`[GET] /combine` <br>
combine all the images annotated previously to one single image and show on the server side

### Folders

`/uploads` <br>
When the `/image` endpoint gets a request, the image in the request body will be saved to this folder for further processing.

`/image_results` <br>
During image recognition, the bounding boxes drawing process will be done in this folder. Annotated images will have filename like `annotated_image_original_filename.jpg`.

## *weights*

Inside is model weights, for image recognition.

<br>

## Usage

Make sure that all necessary packages are installed with proper versions (listed in `/interface server/requirements.txt`). To start the server, use following command inside interface server:
```
python3 main.py
```
The server will be running at `localhost:5007`.
