# app/main/routes.py
from asyncio.log import logger
import hashlib
import uuid
import os
import networkx as nx
from graphviz import Digraph
from werkzeug import Response
from ..cfg_utils import CFGGenerator 
from app.cfg_utils import CFGGenerator
import javalang # For JavaSyntaxError
from flask import app, render_template, request, jsonify, redirect, url_for, current_app, flash
from flask_login import login_required, current_user, logout_user
from . import main_bp # from app/main/__init__.py
from ..models import CodeSubmission, User # from app/models.py
from .. import db # from app/__init__.py
from ..utils import ( # from app/utils.py
    preprocess_code, format_ast, clean_comment,
    extract_methods, extract_classes, compute_hash, build_ast_json
)

@main_bp.route('/generate-cfg', methods=['POST'])
@login_required
def generate_cfg():
    code = request.json.get('code', '')
    
    try:
        # Create CFG generator
        generator = CFGGenerator()
        cfg = generator.generate(code)
        
        # Generate SVG content
        svg_content = generator.visualize()
        
        # Return SVG directly
        return Response(
            svg_content,
            mimetype='image/svg+xml',
            headers={'Content-Disposition': 'inline; filename=cfg.svg'}
        )
    except Exception as e:
        return jsonify({"error": str(e)}), 400

@main_bp.route('/', methods=['GET', 'POST'])
@login_required
def home():
    if request.method == 'POST':
        try:
            code_input = request.json.get('code', '') #
            if not code_input.strip() or code_input == '{{ code_input }}': #
                return jsonify({ #
                    'comments': '<div class="comment-error">Error: No Code Submitted</div>',
                    'ast': '<div class="ast-error">Error: No Code Submitted</div>'
                })

            code_hash = compute_hash(code_input) #

            existing_submission = CodeSubmission.query.filter_by( #
                user_id=current_user.id, #
                code_hash=code_hash, #
                is_success=True #
            ).first()

            if existing_submission: #
                ast_output = existing_submission.ast_content #
                comments_output = existing_submission.comments_content #
            else:
                try:
                    javalang.parse.parse(code_input) #
                except javalang.parser.JavaSyntaxError as e: #
                    line_number = getattr(e.at, 'line', 'unknown') #
                    return jsonify({ #
                        'comments': f'<div class="comment-error">Java Syntax Error (Line {line_number}): {e.description}</div>',
                        'ast': format_ast(code_input) # Still show AST if possible
                    })

                class_structure = extract_classes(code_input) #
                method_structure = extract_methods(code_input) #

                if isinstance(class_structure, dict) and 'error' in class_structure: #
                     return jsonify({'comments': class_structure['error'], 'ast': format_ast(code_input)})
                if isinstance(method_structure, dict) and 'error' in method_structure: #
                     return jsonify({'comments': method_structure['error'], 'ast': format_ast(code_input)})


                ast_output = format_ast(code_input) #
                grouped_comments = {} #

                # Use the structures already extracted
                for class_name, class_code in class_structure.items(): #
                    processed_class = preprocess_code(class_code) #
                    if current_app.hf_pipeline: #
                        result = current_app.hf_pipeline(processed_class) #
                        comment = clean_comment(result[0]['generated_text']) #
                        grouped_comments[class_name] = { #
                            'class_comment': f'<div class="comment-class" id="class_{class_name}">📦 Class: {class_name}\n{comment}</div>',
                            'method_comments': []
                        }

                for class_name, methods in method_structure.items(): #
                    if class_name not in grouped_comments: # Handle classes with no direct code but with methods
                        grouped_comments[class_name] = {'class_comment': '', 'method_comments': []}
                    for method in methods: #
                        processed_method = preprocess_code(method['code']) #
                        if current_app.hf_pipeline: #
                            result = current_app.hf_pipeline(processed_method) #
                            comment = clean_comment(result[0]['generated_text']) #
                            grouped_comments[class_name]['method_comments'].append( #
                                f'<div class="comment-method" id="method_{class_name}_{method["name"]}">◆ {class_name}.{method["name"]}:\n{comment}</div>'
                            )

                comments_output_list = [] #
                for class_data in grouped_comments.values(): #
                    if class_data['class_comment']: comments_output_list.append(class_data['class_comment']) #
                    comments_output_list.extend(class_data['method_comments']) #
                comments_output = '\n'.join(comments_output_list) if comments_output_list else "No comments generated" #

                new_submission = CodeSubmission( #
                    user_id=current_user.id, #
                    code_content=code_input, #
                    submission_name=f"Submission-{uuid.uuid4().hex[:6]}", #
                    ast_content=ast_output, #
                    comments_content=comments_output, #
                    code_hash=code_hash, #
                    is_success=True #
                )
                db.session.add(new_submission) #
                db.session.commit() #

            return jsonify({ #
                'comments': comments_output,
                'ast': ast_output,
                'cfg_supported': True, # Indicate CFG generation is supported
            })

        except Exception as e: #
            current_app.logger.error(f"Server error in home POST: {str(e)}")
            # Attempt to get code_input if it was defined before the error
            code_input_for_error = request.json.get('code', '') if request.is_json else "Unavailable"

            error_submission = CodeSubmission( #
                user_id=current_user.id, #
                code_content=code_input_for_error, #
                submission_name=f"Failed-{uuid.uuid4().hex[:6]}", #
                is_success=False #
            )
            db.session.add(error_submission) #
            db.session.commit() #

            return jsonify({ #
                'comments': f"Error: {str(e)}",
                'ast': "AST generation failed",
                'cfg_supported': False  # Indicate CFG generation is not supported
            }), 500

    # GET request
    return render_template( #
        'index.html',
        comments='',
        ast='',
        code_input=''
    )


@main_bp.route('/dashboard') #
@login_required #
def dashboard(): #
    current_app.logger.debug(f"Loading dashboard for user: {current_user.username}") #
    submissions = CodeSubmission.query.filter_by( #
        user_id=current_user.id, #
        is_success=True #
    ).order_by(CodeSubmission.timestamp.desc()).all() #
    current_app.logger.debug(f"Found {len(submissions)} submissions") #
    return render_template('dashboard.html', #
                         username=current_user.username, #
                         submissions=submissions) #


@main_bp.route('/settings') #
@login_required #
def settings(): #
    return render_template('settings.html') #


@main_bp.route('/delete-account', methods=['POST']) #
@login_required #
def delete_account(): #
    try:
        password = request.form.get('password') #
        if not current_user.check_password(password): #
            # flash('Incorrect password.', 'error')
            return render_template('settings.html', error='Incorrect password') #

        user_to_delete = User.query.get(current_user.id) #
        db.session.delete(user_to_delete) #
        db.session.commit() #
        logout_user() #
        # flash('Your account has been permanently deleted.', 'success')
        return redirect(url_for('main.home')) #
    except Exception as e: #
        db.session.rollback() #
        current_app.logger.error(f"Error deleting account: {e}")
        # flash('Error deleting account.', 'error')
        return render_template('settings.html', error='Error deleting account') #


@main_bp.route('/get-submission/<int:submission_id>') #
@login_required #
def get_submission(submission_id): #
    submission = CodeSubmission.query.filter_by( #
        id=submission_id, #
        user_id=current_user.id #
    ).first_or_404()
    return jsonify({ #
        'code_content': submission.code_content, #
        'ast_content': submission.ast_content, #
        'comments_content': submission.comments_content #
    })


@main_bp.route('/rename-submission/<int:submission_id>', methods=['POST']) #
@login_required #
def rename_submission(submission_id): #
    submission = CodeSubmission.query.filter_by( #
        id=submission_id, #
        user_id=current_user.id #
    ).first_or_404()
    new_name = request.json.get('new_name', 'Unnamed Submission') #
    submission.submission_name = new_name #
    db.session.commit() #
    return jsonify({'status': 'success'}) #


@main_bp.route('/delete-submission/<int:submission_id>', methods=['DELETE']) #
@login_required #
def delete_submission(submission_id): #
    try:
        submission = CodeSubmission.query.filter_by( #
            id=submission_id, #
            user_id=current_user.id #
        ).first_or_404()
        db.session.delete(submission) #
        db.session.commit() #
        current_app.logger.debug(f"Successfully deleted submission {submission_id} for user {current_user.username}") #
        return jsonify({'status': 'success'}) #
    except Exception as e: #
        db.session.rollback() #
        current_app.logger.error(f"Error deleting submission {submission_id}: {str(e)}") #
        return jsonify({'status': 'error', 'message': str(e)}), 500 #
    

# app/routes.py

@main_bp.route('/ast-json', methods=['POST'])
def ast_json():
    code = request.json.get('code', '')
    ast_data = build_ast_json(code)
    return jsonify(ast_data)

@main_bp.route('/process-folder', methods=['POST'])
@login_required
def process_folder():
    try:
        # Get the uploaded files
        uploaded_files = request.files.getlist('files[]')
        
        if not uploaded_files:
            return jsonify({"error": "No files uploaded"}), 400
        
        # Process each file
        results = {}
        for file in uploaded_files:
            if file.filename.endswith('.java'):
                code_content = file.read().decode('utf-8')
                filename = file.filename
                
                # Process the code (similar to your home route)
                try:
                    # Your existing processing logic here
                    ast_output = format_ast(code_content)
                    
                    # Extract classes and methods
                    class_structure = extract_classes(code_content)
                    method_structure = extract_methods(code_content)
                    
                    # Generate comments
                    grouped_comments = {}
                    for class_name, class_code in class_structure.items():
                        processed_class = preprocess_code(class_code)
                        if current_app.hf_pipeline:
                            result = current_app.hf_pipeline(processed_class)
                            comment = clean_comment(result[0]['generated_text'])
                            grouped_comments[class_name] = {
                                'class_comment': f'<div class="comment-class" id="class_{class_name}">📦 Class: {class_name}\n{comment}</div>',
                                'method_comments': []
                            }
                    
                    for class_name, methods in method_structure.items():
                        if class_name not in grouped_comments:
                            grouped_comments[class_name] = {'class_comment': '', 'method_comments': []}
                        for method in methods:
                            processed_method = preprocess_code(method['code'])
                            if current_app.hf_pipeline:
                                result = current_app.hf_pipeline(processed_method)
                                comment = clean_comment(result[0]['generated_text'])
                                grouped_comments[class_name]['method_comments'].append(
                                    f'<div class="comment-method" id="method_{class_name}_{method["name"]}">◆ {class_name}.{method["name"]}:\n{comment}</div>'
                                )
                    
                    comments_output_list = []
                    for class_data in grouped_comments.values():
                        if class_data['class_comment']: 
                            comments_output_list.append(class_data['class_comment'])
                        comments_output_list.extend(class_data['method_comments'])
                    comments_output = '\n'.join(comments_output_list) if comments_output_list else "No comments generated"
                    
                    # Store results for this file
                    results[filename] = {
                        'ast': ast_output,
                        'comments': comments_output,
                        'code': code_content
                    }
                    
                except Exception as e:
                    results[filename] = {
                        'error': str(e)
                    }
        
        return jsonify(results)
        
    except Exception as e:
        return jsonify({"error": str(e)}), 500
    
@main_bp.route("/model")
def model_page():
    return render_template("model.html")