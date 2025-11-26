# app/cfg_utils.py
import javalang
import networkx as nx
from graphviz import Digraph
import os
import re

class CFGGenerator:
    def __init__(self):
        self.cfg = nx.DiGraph()
        self.current_block = None
        self.block_counter = 0
        self.break_targets = []
        self.continue_targets = []
        self.line_map = {}  # Map statements to line numbers
        self.java_code = ""  # Store original code
        self.method_map = {}  # Map method names to method nodes
        self.method_entries = {}  # Map method names to their entry blocks
        self.method_exits = {}  # Map method names to their exit blocks
        self.method_colors = {}  # Map method names to their unique colors
        self.node_method_map = {}  # Map node IDs to method names for coloring
        self.call_stack = []  # Track method call stack: [(method_name, block_id), ...]
        self.in_infinite_loop = False  # Track if we're in an infinite loop context

    def generate(self, java_code: str) -> nx.DiGraph:
        """Generate CFG from Java code"""
        self.java_code = java_code
        try:
            # Try to parse as-is first
            tree = javalang.parse.parse(java_code)
            self._build_line_map(java_code)
            self._process_tree(tree)
            return self.cfg
        except javalang.parser.JavaSyntaxError as e:
            # Check if error is "expected type declaration" at line 1
            error_line = 1
            if e.at:
                if isinstance(e.at, javalang.tokenizer.Position):
                    error_line = e.at.line
                elif hasattr(e.at, 'position') and e.at.position:
                    error_line = e.at.position.line
            
            error_desc = e.description.lower() if e.description else ""
            is_type_declaration_error = (
                "expected type declaration" in error_desc or
                "expected" in error_desc and "declaration" in error_desc
            )
            
            # If error is at line 1 and is about type declaration, wrap in class
            if error_line == 1 and is_type_declaration_error:
                try:
                    # Wrap code in a public class
                    wrapped_code = f"public class nan {{\n{java_code}\n}}"
                    tree = javalang.parse.parse(wrapped_code)
                    self._build_line_map(java_code)  # Use original for line mapping
                    self._process_tree(tree)
                    return self.cfg
                except Exception as wrap_error:
                    # If wrapping also fails, raise original error
                    raise ValueError(f"Java syntax error: {e}")
            
            # If parsing fails for other reasons, try wrapping in a class if it doesn't start with class
            try:
                stripped = java_code.strip()
                if not stripped.startswith('class ') and not stripped.startswith('public class ') and \
                   not stripped.startswith('private class ') and not stripped.startswith('protected class '):
                    # Try wrapping in a dummy class
                    wrapped_code = f"public class nan {{\n{java_code}\n}}"
                    tree = javalang.parse.parse(wrapped_code)
                    self._build_line_map(java_code)  # Use original for line mapping
                    self._process_tree(tree)
                    return self.cfg
            except:
                pass
            
            raise ValueError(f"Java syntax error: {e}")

    def _build_line_map(self, java_code):
        """Map statements to line numbers"""
        lines = java_code.splitlines()
        for i, line in enumerate(lines):
            self.line_map[i+1] = line.strip()

    def _process_tree(self, tree):
        """Process AST to build CFG"""
        # First pass: collect all method declarations
        current_class = None
        method_list = []
        for path, node in tree:
            if isinstance(node, javalang.tree.ClassDeclaration):
                current_class = node.name
            elif isinstance(node, javalang.tree.MethodDeclaration):
                # Store method by class and name
                method_key = f"{current_class}.{node.name}" if current_class else node.name
                self.method_map[method_key] = node
                self.method_map[node.name] = node  # Also store by name only for easier lookup
                method_list.append((node.name, node))
        
        # Assign unique colors to each method
        color_palette = [
            '#FFE5B4',  # Peach
            '#E6E6FA',  # Lavender
            '#B4E6FF',  # Light blue
            '#FFB4E6',  # Light pink
            '#B4FFE6',  # Mint green
            '#FFFFB4',  # Light yellow
            '#E6B4FF',  # Light purple
            '#B4FFB4',  # Light green
            '#FFE6B4',  # Light orange
            '#B4E6E6',  # Light cyan
            '#FFB4B4',  # Light red
            '#B4B4FF',  # Light indigo
            '#FFD4B4',  # Light apricot
            '#D4FFB4',  # Light lime
            '#B4FFD4',  # Light aquamarine
        ]
        for idx, (method_name, method_node) in enumerate(method_list):
            self.method_colors[method_name] = color_palette[idx % len(color_palette)]
        
        # Second pass: process methods
        for path, node in tree:
            if isinstance(node, javalang.tree.MethodDeclaration):
                self._process_method(node)

    def _process_method(self, method_node):
        """Process a method's CFG"""
        start_line = method_node.position.line if method_node.position else "?"
        method_key = method_node.name
        
        # Check if method entry already exists (method was already processed)
        if method_key in self.method_entries:
            return
        
        # Push this method onto the call stack BEFORE creating entry block
        # This ensures the entry block gets this method's color
        self.call_stack.append((method_key, None))
        
        method_entry = self._new_block(f"METHOD ENTRY: {method_node.name}\nLine: {start_line}")
        self.current_block = method_entry
        
        # Store method entry and assign color
        self.method_entries[method_key] = method_entry
        self.node_method_map[method_entry] = method_key
        # Update stack with actual entry block
        self.call_stack[-1] = (method_key, method_entry)

        if method_node.body:
            self._process_block(method_node.body)
            # If method doesn't end with return, create implicit exit
            # Check if last block is already a method exit
            exit_blocks = []
            if self.current_block and "METHOD EXIT" not in self.cfg.nodes[self.current_block].get("label", ""):
                # Check if there's already an exit edge
                has_exit = False
                for _, dst in self.cfg.out_edges(self.current_block):
                    if "METHOD EXIT" in self.cfg.nodes[dst].get("label", ""):
                        has_exit = True
                        exit_blocks.append(dst)
                        break
                if not has_exit:
                    exit_block = self._new_block("METHOD EXIT")
                    self._connect_blocks(self.current_block, exit_block)
                    exit_blocks.append(exit_block)
                    self.node_method_map[exit_block] = method_key
            else:
                # Find all exit blocks
                for node in self.cfg.nodes():
                    if "METHOD EXIT" in self.cfg.nodes[node].get("label", "") and \
                       self._is_reachable_from(method_entry, node):
                        exit_blocks.append(node)
                        if node not in self.node_method_map:
                            self.node_method_map[node] = method_key
            
            # Store method exits
            if exit_blocks:
                self.method_exits[method_key] = exit_blocks
            else:
                # If no exit found, create one
                exit_block = self._new_block("METHOD EXIT")
                if self.current_block:
                    self._connect_blocks(self.current_block, exit_block)
                self.method_exits[method_key] = [exit_block]
                self.node_method_map[exit_block] = method_key
        
        # Pop this method from the call stack when done processing
        if self.call_stack and self.call_stack[-1][0] == method_key:
            self.call_stack.pop()

    def _process_block(self, block_node):
        """Process a block of statements"""
        for stmt in block_node:
            # If we're in an infinite loop context, don't process further statements
            # (they're unreachable and shouldn't be connected to the loop)
            if self.in_infinite_loop:
                # Don't process or connect any statements after infinite loop
                # They are unreachable code
                break
            else:
                self._process_statement(stmt)

    def _process_statement(self, stmt):
        """Process individual statements"""
        if isinstance(stmt, javalang.tree.IfStatement):
            self._process_if_statement(stmt)
        elif isinstance(stmt, javalang.tree.WhileStatement):
            self._process_while_statement(stmt)
        elif isinstance(stmt, javalang.tree.ForStatement):
            self._process_for_statement(stmt)
        elif isinstance(stmt, javalang.tree.BlockStatement):
            self._process_block(stmt.statements)
        elif isinstance(stmt, javalang.tree.ReturnStatement):
            self._add_statement_to_block(stmt)
            # Create end block for return
            end_block = self._new_block("METHOD EXIT")
            self._connect_blocks(self.current_block, end_block)
            self.current_block = end_block
        elif isinstance(stmt, javalang.tree.StatementExpression):
            # Check if this is a method call
            if isinstance(stmt.expression, javalang.tree.MethodInvocation):
                self._process_method_invocation(stmt.expression)
            else:
                self._add_statement_to_block(stmt)
        else:
            self._add_statement_to_block(stmt)

    def _add_statement_to_block(self, stmt):
        """Add statement to current block with line number"""
        line_no = stmt.position.line if stmt.position else "?"
        stmt_type = type(stmt).__name__.replace("Statement", "")
        
        # Get statement text from original code
        stmt_text = ""
        if stmt.position:
            stmt_text = self._get_statement_text(stmt.position.line)
        
        # Add to current block
        if "label" not in self.cfg.nodes[self.current_block]:
            self.cfg.nodes[self.current_block]['label'] = ""
        self.cfg.nodes[self.current_block]['label'] += f"\nL{line_no}: {stmt_text}"

    def _get_statement_text(self, line_no):
        """Get original statement text from line number"""
        if line_no in self.line_map:
            return self.line_map[line_no][:100]  # Truncate long lines
        return ""

    def _process_if_statement(self, if_node):
        """Process if statement"""
        cond_line = if_node.condition.position.line if if_node.condition.position else "?"
        cond_text = self._get_statement_text(cond_line)
        cond_block = self._new_block(f"IF CONDITION\nL{cond_line}: {cond_text}")
        
        # Connect current block to condition
        self._connect_blocks(self.current_block, cond_block)
        
        # Process then branch
        then_block = self._new_block("THEN BRANCH")
        self._connect_blocks(cond_block, then_block)
        prev_block = self.current_block
        self.current_block = then_block
        self._process_statement(if_node.then_statement)
        
        # Create merge point
        merge_block = self._new_block("IF MERGE")
        self._connect_blocks(then_block, merge_block)
        
        # Process else branch if exists
        if if_node.else_statement:
            else_block = self._new_block("ELSE BRANCH")
            self._connect_blocks(cond_block, else_block)
            self.current_block = else_block
            self._process_statement(if_node.else_statement)
            self._connect_blocks(else_block, merge_block)
        else:
            # Connect condition directly to merge block
            self._connect_blocks(cond_block, merge_block)
        
        self.current_block = merge_block

    def _process_while_statement(self, while_node):
        """Process while loop"""
        cond_line = while_node.condition.position.line if while_node.condition.position else "?"
        cond_text = self._get_statement_text(cond_line)
        
        # Check if loop never runs (always false condition)
        never_runs = self._is_always_false_condition(while_node.condition)
        
        if never_runs:
            # Loop never runs - create nodes but don't connect them with arrows
            cond_block = self._new_block(f"WHILE CONDITION\nL{cond_line}: {cond_text}")
            body_block = self._new_block("LOOP BODY")
            
            # Process body to create its nodes (but don't connect)
            saved_block = self.current_block
            self.current_block = body_block
            self._process_statement(while_node.body)
            self.current_block = saved_block  # Restore - don't connect loop nodes
            
            # Create exit block but don't connect from condition
            exit_block = self._new_block("LOOP EXIT")
            # Connect from previous block to exit (skip the loop entirely)
            self._connect_blocks(self.current_block, exit_block)
            self.current_block = exit_block
            return
        
        # Analyze loop termination using condition and body analysis
        is_infinite, reason = self._analyze_loop_termination(while_node.condition, while_node.body)
        
        # Fallback to simple check if analysis didn't find variables
        if not is_infinite:
            is_infinite = self._is_infinite_loop_condition(while_node.condition)
        
        cond_block = self._new_block(f"WHILE CONDITION\nL{cond_line}: {cond_text}")
        
        # Connect current block to condition
        self._connect_blocks(self.current_block, cond_block)
        
        # Create loop body block
        body_block = self._new_block("LOOP BODY")
        # Always connect condition to body (true branch)
        self._connect_blocks(cond_block, body_block)
        
        # Process body
        self.current_block = body_block
        self._process_statement(while_node.body)
        self._connect_blocks(body_block, cond_block)  # Loop back
        
        # Handle exit block based on whether loop is infinite
        if not is_infinite:
            # Normal loop: condition can go to body or exit
            exit_block = self._new_block("LOOP EXIT")
            self._connect_blocks(cond_block, exit_block)
            self.current_block = exit_block
        else:
            # For infinite loops, don't create exit node and don't allow further connections
            # Mark that we're in an infinite loop context - no exit path exists
            self.in_infinite_loop = True
            # Set current_block to cond_block but mark it so no further connections are made
            # The loop only has: previous -> condition -> body -> condition (loop back)
            # No arrow from condition to any exit or subsequent node
            self.current_block = cond_block

    def _process_for_statement(self, for_node):
        """Process for loop"""
        # Create init block
        init_block = self._new_block("FOR INIT")
        self._connect_blocks(self.current_block, init_block)
        
        # Handle different for loop types
        is_foreach = hasattr(for_node, 'control') and isinstance(for_node.control, javalang.tree.EnhancedForControl)
        has_condition = hasattr(for_node, 'control') and hasattr(for_node.control, 'condition') and for_node.control.condition is not None
        
        # Condition block - handle all cases
        cond_line = "?"
        cond_text = "true"
        
        if is_foreach:
            # For-each loop
            cond_line = for_node.position.line if for_node.position else "?"
            iterable = for_node.control.iterable if hasattr(for_node.control, 'iterable') else "?"
            var_name = for_node.control.var.name if hasattr(for_node.control.var, 'name') else "?"
            cond_text = f"for ({var_name} : {iterable})"
        elif has_condition:
            # Standard for loop with condition
            cond_line = for_node.control.condition.position.line if for_node.control.condition.position else "?"
            cond_text = self._get_statement_text(cond_line) if cond_line != "?" else "true"
        else:
            # Infinite loop (no condition)
            cond_text = "true"
        
        cond_block = self._new_block(f"FOR CONDITION\nL{cond_line}: {cond_text}")
        self._connect_blocks(init_block, cond_block)
        
        # Check if loop never runs (always false condition)
        never_runs = False
        if has_condition:
            never_runs = self._is_always_false_condition(for_node.control.condition)
        
        if never_runs:
            # Loop never runs - create nodes but don't connect them with arrows
            body_block = self._new_block("LOOP BODY")
            
            # Process body to create its nodes (but don't connect)
            saved_block = self.current_block
            self.current_block = body_block
            self._process_statement(for_node.body)
            self.current_block = saved_block  # Restore - don't connect loop nodes
            
            # Create exit block but don't connect from condition
            exit_block = self._new_block("LOOP EXIT")
            # Connect from init block to exit (skip the loop entirely)
            self._connect_blocks(init_block, exit_block)
            self.current_block = exit_block
            return
        
        # Create body block
        body_block = self._new_block("LOOP BODY")
        self._connect_blocks(cond_block, body_block)
        
        # Process body
        self.current_block = body_block
        self._process_statement(for_node.body)
        
        # Update block (if exists) - not present in for-each loops
        if not is_foreach and hasattr(for_node.control, 'update') and for_node.control.update:
            update_block = self._new_block("FOR UPDATE")
            self._connect_blocks(body_block, update_block)
            self.current_block = update_block
            # Add update statements
            for update_stmt in for_node.control.update:
                self._add_statement_to_block(update_stmt)
            self._connect_blocks(update_block, cond_block)
        else:
            self._connect_blocks(body_block, cond_block)
        
        # Analyze loop termination for for loops
        is_infinite = False
        if not has_condition:
            # No condition means infinite loop
            is_infinite = True
        elif has_condition:
            # Analyze if condition variables are modified in a way that could terminate
            is_infinite, reason = self._analyze_loop_termination(for_node.control.condition, for_node.body)
            # Also check update statements - if update modifies condition variables, loop might terminate
            if not is_infinite and hasattr(for_node.control, 'update') and for_node.control.update:
                # Check if update modifies condition variables
                condition_vars = self._extract_variables_from_expression(for_node.control.condition)
                for update_stmt in for_node.control.update:
                    modified_vars = self._extract_modified_variables(update_stmt)
                    if condition_vars.intersection(modified_vars):
                        # Condition variable is modified in update - loop can terminate
                        is_infinite = False
                        break
            # Fallback to simple check
            if not is_infinite:
                is_infinite = self._is_infinite_loop_condition(for_node.control.condition)
        
        # Only create exit block if not infinite loop
        if not is_infinite:
            # Normal loop: condition can go to body or exit
            exit_block = self._new_block("LOOP EXIT")
            self._connect_blocks(cond_block, exit_block)
            self.current_block = exit_block
        else:
            # For infinite loops, don't create exit node and don't allow further connections
            # Mark that we're in an infinite loop context - no exit path exists
            self.in_infinite_loop = True
            # Set current_block to cond_block but mark it so no further connections are made
            # The loop only has: previous -> condition -> body -> condition (loop back)
            # No arrow from condition to any exit or subsequent node
            self.current_block = cond_block

    def _new_block(self, label=None):
        """Create a new basic block"""
        block_id = f"B{self.block_counter}"
        self.block_counter += 1
        self.cfg.add_node(block_id, label=label or "BLOCK")
        
        # Assign color based on current method in call stack
        if self.call_stack:
            current_method = self.call_stack[-1][0]
            self.node_method_map[block_id] = current_method
        else:
            # If no method in stack, try to find from context
            # This handles cases where blocks are created outside method context
            pass
        
        return block_id

    def _connect_blocks(self, from_block, to_block):
        """Connect two blocks in the CFG"""
        # Don't create edges if we're in an infinite loop context and trying to connect from the loop
        if self.in_infinite_loop and from_block != to_block:
            # Check if from_block is part of an infinite loop (has "WHILE CONDITION" or "FOR CONDITION" in label)
            from_label = self.cfg.nodes[from_block].get("label", "")
            if "WHILE CONDITION" in from_label or "FOR CONDITION" in from_label:
                # Don't create edge from infinite loop condition to anything outside the loop
                return
        self.cfg.add_edge(from_block, to_block)
    
    def _extract_variables_from_expression(self, expr):
        """Extract variable names from an expression"""
        variables = set()
        
        if isinstance(expr, javalang.tree.MemberReference):
            variables.add(expr.member)
        elif isinstance(expr, javalang.tree.BinaryOperation):
            variables.update(self._extract_variables_from_expression(expr.operandl))
            variables.update(self._extract_variables_from_expression(expr.operandr))
        elif isinstance(expr, javalang.tree.UnaryOperation):
            variables.update(self._extract_variables_from_expression(expr.operand))
        elif isinstance(expr, javalang.tree.Cast):
            if expr.expression:
                variables.update(self._extract_variables_from_expression(expr.expression))
        elif isinstance(expr, javalang.tree.MethodInvocation):
            # Method calls might modify state, but we'll focus on direct variable access
            pass
        
        return variables
    
    def _extract_modified_variables(self, stmt):
        """Extract variables that are modified in a statement"""
        modified = set()
        
        if isinstance(stmt, javalang.tree.StatementExpression):
            expr = stmt.expression
            if isinstance(expr, javalang.tree.Assignment):
                # Extract left-hand side variable
                if isinstance(expr.expressionl, javalang.tree.MemberReference):
                    modified.add(expr.expressionl.member)
            elif isinstance(expr, (javalang.tree.PostfixExpression, javalang.tree.PrefixExpression)):
                # Postfix/prefix operations like i++, --j
                if isinstance(expr.expression, javalang.tree.MemberReference):
                    modified.add(expr.expression.member)
        elif isinstance(stmt, javalang.tree.BlockStatement):
            for sub_stmt in stmt.statements:
                modified.update(self._extract_modified_variables(sub_stmt))
        
        return modified
    
    def _analyze_loop_termination(self, condition_node, loop_body):
        """
        Analyze if a loop can terminate by checking if condition variables are modified
        in a way that could make the condition false.
        Returns: (is_infinite, reason)
        """
        if condition_node is None:
            return True, "No condition"
        
        # Check for literal true
        if isinstance(condition_node, javalang.tree.Literal):
            if condition_node.value == "true" or condition_node.value == "1":
                return True, "Always true literal"
        
        # Check for binary operations that are always true
        if isinstance(condition_node, javalang.tree.BinaryOperation):
            # Check for expressions like 1==1, true==true, etc.
            if isinstance(condition_node.operandl, javalang.tree.Literal) and \
               isinstance(condition_node.operandr, javalang.tree.Literal):
                left_val = str(condition_node.operandl.value)
                right_val = str(condition_node.operandr.value)
                op = condition_node.operator
                
                if op == "==" and left_val == right_val:
                    return True, "Always true comparison"
                if op == "!=" and left_val != right_val:
                    return True, "Always true comparison"
        
        # Extract variables from condition
        condition_vars = self._extract_variables_from_expression(condition_node)
        if not condition_vars:
            # No variables in condition, check if it's always true
            cond_text = ""
            if condition_node.position:
                cond_text = self._get_statement_text(condition_node.position.line).lower()
                cond_clean = re.sub(r'\s+', '', cond_text)
                cond_only = re.sub(r'^(while|for)\s*\(', '', cond_clean)
                cond_only = re.sub(r'\)\s*\{?$', '', cond_only)
                
                infinite_patterns = ["true", "1==1", "true==true", "1!=0", "true!=false", 
                                   "(true)", "(1==1)", "1<2", "2>1", "true||false", "1", 
                                   "true&&true", "!false"]
                if cond_only in infinite_patterns or cond_clean in infinite_patterns:
                    return True, "Always true pattern"
            return False, "No variables to analyze"
        
        # Extract modified variables from loop body
        modified_vars = set()
        if loop_body:
            if isinstance(loop_body, javalang.tree.BlockStatement):
                for stmt in loop_body.statements:
                    modified_vars.update(self._extract_modified_variables(stmt))
            else:
                modified_vars.update(self._extract_modified_variables(loop_body))
        
        # Check if any condition variable is modified
        condition_vars_modified = condition_vars.intersection(modified_vars)
        
        if not condition_vars_modified:
            # Condition variables are not modified in loop body - likely infinite
            # Unless condition is checking something external
            return True, f"Condition variables {condition_vars} not modified in loop body"
        
        # Analyze the direction of modification relative to condition
        # This is a simplified analysis - we check if the modification could lead to termination
        # For example: while (i > 0) with i++ would be infinite, but i-- would terminate
        
        # Get condition operator and operands
        if isinstance(condition_node, javalang.tree.BinaryOperation):
            op = condition_node.operator
            left = condition_node.operandl
            right = condition_node.operandr
            
            # Check if left is a variable and right is a constant
            if isinstance(left, javalang.tree.MemberReference) and isinstance(right, javalang.tree.Literal):
                var_name = left.member
                if var_name in condition_vars_modified:
                    # Check modification direction (simplified - would need deeper analysis)
                    # For now, if variable is modified, assume it might terminate
                    # unless we can prove otherwise
                    return False, f"Variable {var_name} is modified"
            
            # Check if right is a variable and left is a constant
            if isinstance(right, javalang.tree.MemberReference) and isinstance(left, javalang.tree.Literal):
                var_name = right.member
                if var_name in condition_vars_modified:
                    return False, f"Variable {var_name} is modified"
        
        # If we can't determine, assume it might terminate (conservative approach)
        return False, "Unable to determine - assuming might terminate"
    
    def _is_always_false_condition(self, condition_node):
        """Check if a condition is always false (loop never runs)"""
        if condition_node is None:
            return False
        
        # Check for literal false
        if isinstance(condition_node, javalang.tree.Literal):
            if condition_node.value == "false" or condition_node.value == "0":
                return True
        
        # Check for binary operations that are always false
        if isinstance(condition_node, javalang.tree.BinaryOperation):
            if isinstance(condition_node.operandl, javalang.tree.Literal) and \
               isinstance(condition_node.operandr, javalang.tree.Literal):
                left_val = str(condition_node.operandl.value)
                right_val = str(condition_node.operandr.value)
                op = condition_node.operator
                
                if op == "==" and left_val != right_val:
                    return True
                if op == "!=" and left_val == right_val:
                    return True
                if op == "<" and float(left_val) >= float(right_val) if left_val.isdigit() and right_val.isdigit() else False:
                    return True
                if op == ">" and float(left_val) <= float(right_val) if left_val.isdigit() and right_val.isdigit() else False:
                    return True
        
        # Check condition text for common never-run patterns
        if condition_node.position:
            cond_text = self._get_statement_text(condition_node.position.line).lower()
            cond_clean = re.sub(r'\s+', '', cond_text)
            cond_only = re.sub(r'^(while|for)\s*\(', '', cond_clean)
            cond_only = re.sub(r'\)\s*\{?$', '', cond_only)
            
            never_run_patterns = ["false", "0", "1==0", "false==true", "1>2", "2<1", 
                                 "(false)", "(1==0)", "true&&false", "!true"]
            if cond_only in never_run_patterns or cond_clean in never_run_patterns:
                return True
        
        return False
    
    def _is_infinite_loop_condition(self, condition_node):
        """Check if a condition is always true (infinite loop) - simplified version"""
        if condition_node is None:
            return True
        
        # Check for literal true
        if isinstance(condition_node, javalang.tree.Literal):
            if condition_node.value == "true" or condition_node.value == "1":
                return True
        
        # Check for binary operations that are always true
        if isinstance(condition_node, javalang.tree.BinaryOperation):
            if isinstance(condition_node.operandl, javalang.tree.Literal) and \
               isinstance(condition_node.operandr, javalang.tree.Literal):
                left_val = str(condition_node.operandl.value)
                right_val = str(condition_node.operandr.value)
                op = condition_node.operator
                
                if op == "==" and left_val == right_val:
                    return True
                if op == "!=" and left_val != right_val:
                    return True
        
        # Check condition text for common infinite loop patterns
        if condition_node.position:
            cond_text = self._get_statement_text(condition_node.position.line).lower()
            cond_clean = re.sub(r'\s+', '', cond_text)
            cond_only = re.sub(r'^(while|for)\s*\(', '', cond_clean)
            cond_only = re.sub(r'\)\s*\{?$', '', cond_only)
            
            infinite_patterns = ["true", "1==1", "true==true", "1!=0", "true!=false", 
                               "(true)", "(1==1)", "1<2", "2>1", "true||false", "1", 
                               "true&&true", "!false"]
            if cond_only in infinite_patterns or cond_clean in infinite_patterns:
                return True
        
        return False
    
    def _process_method_invocation(self, invocation_node):
        """Process a method invocation - connect caller directly to callee and back (no intermediate nodes)"""
        method_name = invocation_node.member if hasattr(invocation_node, 'member') else "?"
        
        # Save caller's current block and method (where the call happens)
        caller_block = self.current_block
        caller_method = self.call_stack[-1][0] if self.call_stack else None
        
        # Add the method call statement to the current block
        line_no = invocation_node.position.line if invocation_node.position else "?"
        call_text = self._get_statement_text(line_no)
        if "label" not in self.cfg.nodes[caller_block]:
            self.cfg.nodes[caller_block]['label'] = ""
        self.cfg.nodes[caller_block]['label'] += f"\nL{line_no}: {call_text}"
        
        # Check if the method exists and has been processed
        if method_name in self.method_map:
            # Ensure the method has been processed (lazy processing)
            if method_name not in self.method_entries:
                # Save current state
                saved_block = self.current_block
                # Process the method now (this will push it onto the stack and then pop it)
                self._process_method(self.method_map[method_name])
                # Restore caller's block (the method processing changed current_block)
                self.current_block = saved_block
        
        # Check if we have the method entry stored
        if method_name in self.method_entries:
            # Connect caller block directly to method entry (no intermediate call node)
            method_entry = self.method_entries[method_name]
            self._connect_blocks(caller_block, method_entry)
            
            # Push the called method onto the call stack
            # This ensures that when we process blocks in the called method's context,
            # they get the called method's color
            # Note: The called method's blocks are already created, but we need to
            # ensure exit blocks connect back with proper color context
            self.call_stack.append((method_name, method_entry))
            
            # Create a continuation block for after the method returns
            # Temporarily pop the called method to get caller's color for continuation block
            self.call_stack.pop()
            continuation_block = self._new_block("")
            # Ensure continuation block has caller's color
            if caller_method:
                self.node_method_map[continuation_block] = caller_method
            
            # Connect all method exits directly back to continuation block (no intermediate return node)
            # The exit blocks should already have the called method's color
            if method_name in self.method_exits:
                for exit_block in self.method_exits[method_name]:
                    self._connect_blocks(exit_block, continuation_block)
                    # Ensure exit blocks have the called method's color
                    if exit_block not in self.node_method_map:
                        self.node_method_map[exit_block] = method_name
            else:
                # If no exits stored, try to find them by searching from method entry
                exit_blocks = []
                for node in self.cfg.nodes():
                    if "METHOD EXIT" in self.cfg.nodes[node].get("label", "") and \
                       self._is_reachable_from(method_entry, node):
                        exit_blocks.append(node)
                        self._connect_blocks(node, continuation_block)
                        # Ensure exit blocks have the called method's color
                        if node not in self.node_method_map:
                            self.node_method_map[node] = method_name
                # Store found exits for future use
                if exit_blocks:
                    self.method_exits[method_name] = exit_blocks
            
            # Set current block to continuation (execution continues here after method returns)
            # The continuation block already has the caller's color
            self.current_block = continuation_block
        else:
            # Method not found - execution just continues from current block
            # The method call statement is already added to caller_block
            pass
    
    def _is_reachable_from(self, start_node, target_node):
        """Check if target_node is reachable from start_node using DFS"""
        visited = set()
        stack = [start_node]
        
        while stack:
            node = stack.pop()
            if node == target_node:
                return True
            if node in visited:
                continue
            visited.add(node)
            for _, dst in self.cfg.out_edges(node):
                if dst not in visited:
                    stack.append(dst)
        return False

    def visualize(self, format="svg"):
        """Generate a visual representation of the CFG and return SVG content"""
        dot = Digraph(format=format)
        dot.attr('node', shape='box', style='rounded,filled', fontname='Courier')
        dot.attr('edge', arrowhead='vee')
        
        # Default color for nodes without method assignment
        default_color = '#e0f7fa'
        
        for node in self.cfg.nodes():
            label = self.cfg.nodes[node].get("label", node)
            
            # Get color for this node based on method assignment
            color = default_color
            if node in self.node_method_map:
                method_name = self.node_method_map[node]
                if method_name in self.method_colors:
                    color = self.method_colors[method_name]
            
            dot.node(node, label=label, fillcolor=color)
        
        for src, dst in self.cfg.edges():
            dot.edge(src, dst)
        
        # Render to bytes and return SVG content
        svg_bytes = dot.pipe()
        return svg_bytes.decode('utf-8')
    