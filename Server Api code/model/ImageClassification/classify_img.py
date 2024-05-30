from cv2 import resize,INTER_NEAREST
from os import path
from numpy import float32,newaxis
from tensorflow.keras.models import load_model
    
model = load_model(path.abspath('../model/ImageClassification/model.h5'),compile=False)
model.compile()
def classify(img):
    class_list=["Bhairavnath","nyatapola","siddhilaxmi","gopinath"]
    x =float32(img)
    #img = cv2.resize(img, (256, 256), interpolation = cv2.INTER_NEAREST)
    x = resize(x, (224, 224), interpolation = INTER_NEAREST)
    #plt.show()
    #img.shape

    # converting to array
    

    # normalizing value to range of 0 and 1
    x = x / 255

    # adding 1 dimension to 3 dimension matraix to create touple of 4
    x = x[newaxis, :]
    # making output prediction
    result = []
    result = model.predict(x)
    return class_list[list(result[0]).index(max(result[0]))]