# CZ3004-SC2079-MDP-Image Recognition

This is image recognition part for group 24 MDP.

## Table of Content

- [Information](#information)
- [Usage](#usage)

<br>

## Information
---

## *Interface server*

API Endpoint
<ol>
  <li>
    [GET] /status <br>
    health checking
  </li>
  <li>
    [POST] /image <br>
    main image recognition function
  </li>

</ol>

## *weights*

Inside is model weights, for image recognition.

<br>

## Usage
---

Make sure that all necessary packages are installed with proper versions (listed in `/interface server/requirements.txt`). To start the server, use following command inside interface server:
```
python3 main.py
```
The server will be running at `localhost:5007`.

