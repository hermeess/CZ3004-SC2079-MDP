from PIL import Image
import torch

def load_model():
    model = torch.hub.load('./', 'custom', path='yolo_model.pt', source='local')
    return model

def rec_image(image, model):
    try:
        # load image
        img = Image.open(os.path.join('uploads', image))

        results = model(img)

        # TODO save results

        df_results = results.pandas().xyxy[0]
        df_results['bboxHeight'] = df_results['ymax'] - df_results['ymin']
        df_results['bboxWeight'] = df_results['xmax'] - df_results['xmin']
        df_results['bboxArea'] = df_results['bboxHeight'] * df_results['bboxWeight']

        # sort list by bbox area
        df_results = df_results.sort_values('bboxArea', ascending = False)

        # TODO ignore bullseye need or not?

        rec_result = 'NA'

        if len(df_results) == 1:
            rec_result = df_results.iloc[0]

        elif len(df_results) > 1:
            rec_shortlist = []
            current_area = df_results.iloc[0]['bboxArea']
            for _, row in df_results.iterrows():
                if row['confidence'] > 0.5 and (row['bboxArea' >= current_area*0.8]):
                    rec_shortlist.append(row)
                    current_area = row['bboxArea']

            if len(rec_shortlist) == 1:
                rec_result = rec_shortlist[0]
            else:
                # TODO how to choose if multiple rec results
                rec_result = rec_shortlist[0]
    except:
        print("Final result: NA")
        return 'NA'
        