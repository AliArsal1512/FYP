# app/config.py
import os

base_dir = os.path.abspath(os.path.dirname(__file__))

class Config:
    SECRET_KEY = os.environ.get('SECRET_KEY') or '151214' # Change in production!
    SQLALCHEMY_DATABASE_URI = os.environ.get('DATABASE_URL') or \
        'sqlite:///' + os.path.join(base_dir, '..', 'users.db') # Place DB outside 'app'
    SQLALCHEMY_TRACK_MODIFICATIONS = False
    MODEL_PATH = "D:/uni/FYP2/SEBIS" # Or get from environment variable