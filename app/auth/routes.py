# app/auth/routes.py
from flask import render_template, redirect, url_for, request, flash, current_app
from flask_login import login_user, logout_user, current_user, login_required
from sqlalchemy.exc import IntegrityError
from . import auth_bp # from app/auth/__init__.py
from ..models import User # from app/models.py
from .. import db # from app/__init__.py

@auth_bp.route('/login', methods=['GET', 'POST']) #
def login(): #
    if current_user.is_authenticated: #
        return redirect(url_for('main.dashboard')) # Reference dashboard in 'main' blueprint
    if request.method == 'POST': #
        username = request.form.get('username') #
        password = request.form.get('password') #
        user = User.query.filter_by(username=username).first() #
        if user and user.check_password(password): #
            login_user(user) #
            return redirect(url_for('main.dashboard')) #
        # flash('Invalid username or password', 'error') # Use flash for messages
        return render_template('login.html', error='Invalid username or password') #
    return render_template('login.html') #

@auth_bp.route('/signup', methods=['GET', 'POST']) #
def signup(): #
    if current_user.is_authenticated: #
        return redirect(url_for('main.dashboard')) #
    if request.method == 'POST': #
        username = request.form.get('username').strip() #
        email = request.form.get('email').strip() #
        password = request.form.get('password') #

        existing_user = User.query.filter((User.username == username) | (User.email == email)).first() #
        if existing_user: #
            error_msg = 'Username already taken.' if existing_user.username == username else 'Email already registered.' #
            # flash(error_msg, 'error')
            return render_template('signup.html', error=error_msg) #
        try:
            new_user = User(username=username, email=email) #
            new_user.set_password(password) #
            db.session.add(new_user) #
            db.session.commit() #
            login_user(new_user) #
            return redirect(url_for('main.dashboard')) #
        except IntegrityError: #
            db.session.rollback() #
            # flash('Registration failed due to conflicting credentials.', 'error')
            return render_template('signup.html', error='Registration failed. Please try again.') #
        except Exception as e: #
            db.session.rollback() #
            current_app.logger.error(f"Signup error: {e}")
            # flash('An error occurred during registration.', 'error')
            return render_template('signup.html', error='An error occurred. Please try again.') #
    return render_template('signup.html') #

@auth_bp.route('/logout') #
@login_required #
def logout(): #
    logout_user() #
    # flash('You have been logged out.', 'info')
    return redirect(url_for('main.home')) # Redirect to home in 'main' blueprint