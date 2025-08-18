# app/__init__.py
import os
from flask import Flask
from flask_sqlalchemy import SQLAlchemy
from flask_login import LoginManager
from .config import Config # We'll create this file next

# Initialize extensions
db = SQLAlchemy()
login_manager = LoginManager()
login_manager.login_view = 'auth.login' # Refers to the 'login' route in the 'auth' blueprint

# Import models here to avoid circular imports when db.create_all() is called
# This needs to be after db is defined and before create_app returns if using create_all in create_app
from .models import User, CodeSubmission

def create_app(config_class=Config):
    base_dir = os.path.dirname(os.path.abspath(__file__))
    app = Flask(__name__,
                template_folder=os.path.join(base_dir, 'templates'),
                static_folder=os.path.join(base_dir, 'static'),
                instance_relative_config=True) # For instance folder config

    app.config.from_object(config_class)
    app.config['UPLOAD_FOLDER'] = os.path.join(app.static_folder, 'cfg_images')
    # Create the folder if it doesn't exist
    os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)

    # Optionally load instance config
    # app.config.from_pyfile('config.py', silent=True) # if you have instance/config.py

    db.init_app(app)
    login_manager.init_app(app)

    @login_manager.user_loader
    def load_user(user_id):
        return User.query.get(int(user_id))

    # Register blueprints
    from .auth.routes import auth_bp
    from .main.routes import main_bp

    app.register_blueprint(auth_bp, url_prefix='/auth') # All auth routes will be /auth/login, /auth/signup etc.
    app.register_blueprint(main_bp)

    with app.app_context():
        db.create_all() # Create database tables

    # Initialize ML Pipeline (consider moving to a dedicated module if complex)
    try:
        from transformers import AutoTokenizer, AutoModelForSeq2SeqLM, pipeline as hf_pipeline
        import torch

        MODEL_PATH = app.config.get("MODEL_PATH", "D:/uni/FYP2/SEBIS") # Get from config
        DEVICE = 0 if torch.cuda.is_available() else -1

        tokenizer = AutoTokenizer.from_pretrained(
            MODEL_PATH,
            model_max_length=512,
            truncation=True,
            padding="max_length"
        )
        model = AutoModelForSeq2SeqLM.from_pretrained(MODEL_PATH)

        app.hf_pipeline = hf_pipeline( # Store the pipeline on the app object
            "text2text-generation",
            model=model,
            tokenizer=tokenizer,
            device=DEVICE,
            max_length=512,
            truncation=True,
            num_beams=4,
            no_repeat_ngram_size=3
        )
        print("Hugging Face pipeline initialized successfully.")
    except Exception as e:
        print(f"Model initialization error: {str(e)}")
        app.hf_pipeline = None

    return app