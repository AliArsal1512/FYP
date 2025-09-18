# app/utils.py
import javalang
import hashlib
from flask import current_app # To access app.hf_pipeline

def preprocess_code(code: str) -> str:
    # ... (your preprocess_code function)
    return (
        code.replace('\t', ' ')
        .replace('\n', ' ')
        .replace('\r', ' ')
        .replace('  ', ' ')
        .strip()
    )


def format_ast(java_code: str) -> str: #
    # ... (your format_ast function)
    # Make sure to handle imports like javalang at the top of this file
    try:
        tree = javalang.parse.parse(java_code)
        output = ['<div class="ast-tree">']

        for _, class_node in tree.filter(javalang.tree.ClassDeclaration):
            class_name = class_node.name
            output.append(
                f'<div class="ast-class" data-class="{class_name}" '
                f'onclick="showClassComments(\'{class_name}\')">'
                f'📦 Class: {class_name}'
                '</div>'
            )

            if class_node.fields:
                output.append('<div class="ast-section">├─ 🟣 Fields:')
                for field in class_node.fields:
                    modifiers = " ".join(field.modifiers) if field.modifiers else ""
                    field_type = field.type.name if field.type else "Unknown"
                    for declarator in field.declarators:
                        output.append(
                            f'<div class="ast-field">│   ├─ {modifiers} {field_type} {declarator.name}</div>'
                        )
                output.append('</div>')

            if class_node.methods:
                output.append('<div class="ast-section">└─ 🔧 Methods:')
                for i, method in enumerate(class_node.methods):
                    is_last_method = i == len(class_node.methods) - 1
                    method_prefix = "    " if is_last_method else "│   "

                    modifiers = " ".join(method.modifiers) if method.modifiers else ""
                    return_type = method.return_type.name if method.return_type else "void"
                    params = ", ".join([f"{p.type.name} {p.name}" for p in method.parameters]) if method.parameters else ""

                    output.append(
                        f'<div class="ast-method" data-class="{class_name}" data-method="{method.name}" '
                        f'onclick="showMethodComments(\'{class_name}\', \'{method.name}\')">'
                        f'    {method_prefix} {"└─" if is_last_method else "├─"} 🔹 {modifiers} {return_type} {method.name}({params})'
                        '</div>'
                    )

                    method_vars, loops = _process_method_body(method.body) #

                    if method_vars:
                        output.append(f'<div class="ast-subsection">{method_prefix} │ └─ 🟡 Variables:')
                        for var in method_vars:
                            output.append(f'<div class="ast-var">{method_prefix} │     ├─ {var}</div>')
                        output.append('</div>')

                    if loops:
                        output.append(f'<div class="ast-subsection">{method_prefix} └─ 🔁 Loops:')
                        for loop in loops:
                            output.append(f'<div class="ast-loop">{method_prefix}       ├─ {loop["type"]} Loop')
                            if loop['vars']:
                                for var in loop['vars']:
                                    output.append(f'<div class="ast-loop-var">{method_prefix}       │   ├─ 🟠 {var}</div>')
                            else:
                                output.append(f'<div class="ast-loop-empty">{method_prefix}       │   └─ (no variables)</div>')
                        output.append('</div>')

                output.append('</div>')

        output.append('</div>')
        return '\n'.join(output)

    except javalang.parser.JavaSyntaxError as e:
        line_number = 'unknown'
        if e.at:
            if isinstance(e.at, javalang.tokenizer.Position):
                line_number = e.at.line
            elif hasattr(e.at, 'position'):
                line_number = e.at.position.line
        return f'<div class="ast-error">Java Syntax Error (Line {line_number}): {e.description}</div>'


def _process_method_body(body): #
    # ... (your _process_method_body function)
    method_vars = []
    loops = []

    if not body:
        return method_vars, loops

    if isinstance(body, javalang.tree.BlockStatement):
        statements = body.statements
    else:
        statements = [body] if body else []

    def _collect_loop_vars(loop_node):
        loop_vars = []
        if isinstance(loop_node, javalang.tree.ForStatement):
            if loop_node.control and loop_node.control.init:
                for init in loop_node.control.init:
                    if isinstance(init, javalang.tree.VariableDeclaration):
                        loop_vars.extend([f"{init.type.name} {d.name}" for d in init.declarators])

        if loop_node.body:
            body_statements = loop_node.body.statements if isinstance(loop_node.body, javalang.tree.BlockStatement) else [loop_node.body]
            for stmt in body_statements:
                if isinstance(stmt, javalang.tree.LocalVariableDeclaration):
                    loop_vars.extend([f"{stmt.type.name} {d.name}" for d in stmt.declarators])
        return loop_vars

    if body: # This condition might be redundant due to the initial check
        for stmt in statements: # Use 'statements' which is guaranteed to be a list
            if isinstance(stmt, javalang.tree.LocalVariableDeclaration):
                method_vars.extend([f"{stmt.type.name} {d.name}" for d in stmt.declarators])

            if isinstance(stmt, (javalang.tree.ForStatement,
                                  javalang.tree.WhileStatement,
                                  javalang.tree.DoStatement)):
                loop_type = stmt.__class__.__name__.replace("Statement", "")
                loops.append({
                    "type": loop_type,
                    "vars": _collect_loop_vars(stmt)
                })

    return method_vars, loops


def clean_comment(raw_comment: str) -> str: #
    sentences = [s.strip() for s in raw_comment.split('.') if s.strip()]
    filtered = []

    for sentence in sentences:
        filtered.append(sentence[0].upper() + sentence[1:])

    return '. '.join(filtered) + '.' if filtered else "No comment generated"


def extract_methods(java_code: str) -> dict: #
    # Remember to return jsonify errors or raise custom exceptions to be handled by routes
    try:
        tree = javalang.parse.parse(java_code)
        lines = java_code.splitlines()
        method_map = {}

        for _, class_node in tree.filter(javalang.tree.ClassDeclaration):
            class_name = class_node.name
            method_map[class_name] = []

            for method in class_node.methods:
                if method.body is None:
                    continue

                start_line = method.position.line - 1 if method.position else 0

                brace_count = 0
                method_lines = []
                in_method_body = False

                for i in range(start_line, len(lines)):
                    line = lines[i]

                    if '{' in line and not in_method_body:
                        in_method_body = True
                        brace_count += line.count('{')
                        method_lines.append(line)
                        continue

                    if in_method_body:
                        method_lines.append(line)
                        brace_count += line.count('{')
                        brace_count -= line.count('}')

                        if brace_count == 0:
                            break

                method_code = '\n'.join(method_lines).strip()

                if not method_code or '{' not in method_code:
                    continue

                method_map[class_name].append({
                    'name': method.name,
                    'code': method_code
                })
        return method_map
    except javalang.parser.JavaSyntaxError as e:
        # Consider raising an error instead of returning jsonify here,
        # so the route can handle the HTTP response.
        # For now, returning a dict that the route can jsonify.
        line_number = 'unknown'
        if e.at:
            if isinstance(e.at, javalang.tokenizer.Position): line_number = e.at.line
            elif hasattr(e.at, 'position'): line_number = e.at.position.line
        return {'error': f'Java Syntax Error (Line {line_number}): {e.description}'}


def extract_classes(java_code: str) -> dict: #
    # ... (your extract_classes function)
    try:
        tree = javalang.parse.parse(java_code)
        lines = java_code.splitlines()
        class_map = {}

        for _, class_node in tree.filter(javalang.tree.ClassDeclaration):
            class_name = class_node.name
            start_line = class_node.position.line - 1 if class_node.position else 0

            brace_count = 0
            class_lines = []
            in_class = False

            for i in range(start_line, len(lines)):
                line = lines[i]
                class_lines.append(line.strip())

                brace_count += line.count('{')
                brace_count -= line.count('}')

                if not in_class and '{' in line:
                    in_class = True
                    # Reset brace_count when first open brace of class is found.
                    # This assumes classes are not nested in a way that confuses this simple counter.
                    brace_count = line.count('{') - line.count('}')
                elif in_class and brace_count <= 0: # <= 0 to handle case where open and close are on same line
                    # If we started with a brace, we need to find its match.
                    # If the first line had more '{' than '}', this logic might need adjustment.
                    # A more robust way is to count from the class declaration line's first '{'.
                    break

            class_code = ' '.join(class_lines).strip()
            class_map[class_name] = class_code
        return class_map
    except javalang.parser.JavaSyntaxError as e:
        line_number = 'unknown'
        if e.at:
            if isinstance(e.at, javalang.tokenizer.Position): line_number = e.at.line
            elif hasattr(e.at, 'position'): line_number = e.at.position.line
        return {'error': f'Java Syntax Error (Line {line_number}): {e.description}'}


def compute_hash(code): #
    return hashlib.sha256(code.encode('utf-8')).hexdigest()


# utils.py
def build_ast_json(java_code: str) -> dict:
    try:
        tree = javalang.parse.parse(java_code)
        classes = []
        
        # Extract classes and methods first to generate comments
        class_structure = extract_classes(java_code)
        method_structure = extract_methods(java_code)
        
        # Generate comments for classes
        class_comments = {}
        for class_name, class_code in class_structure.items():
            if not isinstance(class_code, str):  # Skip error responses
                continue
            processed_class = preprocess_code(class_code)
            if current_app.hf_pipeline:
                result = current_app.hf_pipeline(processed_class)
                comment = clean_comment(result[0]['generated_text'])
                class_comments[class_name] = comment
        
        # Generate comments for methods
        method_comments = {}
        for class_name, methods in method_structure.items():
            if not isinstance(methods, list):  # Skip error responses
                continue
            for method in methods:
                processed_method = preprocess_code(method['code'])
                if current_app.hf_pipeline:
                    result = current_app.hf_pipeline(processed_method)
                    comment = clean_comment(result[0]['generated_text'])
                    method_comments[(class_name, method['name'])] = comment

        # Build AST with comments
        for _, class_node in tree.filter(javalang.tree.ClassDeclaration):
            class_data = {
                "name": class_node.name,
                "type": "class",
                "comment": class_comments.get(class_node.name, "No comment available"),
                "children": []
            }

            # Fields
            if class_node.fields:
                fields_node = {
                    "name": "Fields",
                    "type": "fields",
                    "children": []
                }
                for field in class_node.fields:
                    modifiers = " ".join(field.modifiers) if field.modifiers else ""
                    field_type = field.type.name if field.type else "Unknown"
                    for declarator in field.declarators:
                        field_data = {
                            "name": f"{modifiers} {field_type} {declarator.name}",
                            "type": "field"
                        }
                        fields_node["children"].append(field_data)
                class_data["children"].append(fields_node)

            # Methods
            if class_node.methods:
                methods_node = {
                    "name": "Methods",
                    "type": "methods",
                    "children": []
                }
                for method in class_node.methods:
                    modifiers = " ".join(method.modifiers) if method.modifiers else ""
                    return_type = method.return_type.name if method.return_type else "void"
                    params = ", ".join([f"{p.type.name} {p.name}" for p in method.parameters]) if method.parameters else ""
                    method_data = {
                        "name": f"{modifiers} {return_type} {method.name}({params})",
                        "type": "method",
                        "comment": method_comments.get((class_node.name, method.name), "No comment available")
                    }
                    methods_node["children"].append(method_data)
                class_data["children"].append(methods_node)

            classes.append(class_data)
        
        return {"name": "Root", "type": "root", "children": classes}
    
    except javalang.parser.JavaSyntaxError as e:
        return {"error": f"Java Syntax Error: {e.description}"}