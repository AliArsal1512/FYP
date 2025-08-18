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

    def generate(self, java_code: str) -> nx.DiGraph:
        """Generate CFG from Java code"""
        self.java_code = java_code
        try:
            tree = javalang.parse.parse(java_code)
            self._build_line_map(java_code)
            self._process_tree(tree)
            return self.cfg
        except javalang.parser.JavaSyntaxError as e:
            raise ValueError(f"Java syntax error: {e}")

    def _build_line_map(self, java_code):
        """Map statements to line numbers"""
        lines = java_code.splitlines()
        for i, line in enumerate(lines):
            self.line_map[i+1] = line.strip()

    def _process_tree(self, tree):
        """Process AST to build CFG"""
        for path, node in tree:
            if isinstance(node, javalang.tree.MethodDeclaration):
                self._process_method(node)

    def _process_method(self, method_node):
        """Process a method's CFG"""
        start_line = method_node.position.line if method_node.position else "?"
        method_entry = self._new_block(f"METHOD ENTRY: {method_node.name}\nLine: {start_line}")
        self.current_block = method_entry

        if method_node.body:
            self._process_block(method_node.body)

    def _process_block(self, block_node):
        """Process a block of statements"""
        for stmt in block_node:
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
        cond_block = self._new_block(f"WHILE CONDITION\nL{cond_line}: {cond_text}")
        
        # Connect current block to condition
        self._connect_blocks(self.current_block, cond_block)
        
        # Create loop body block
        body_block = self._new_block("LOOP BODY")
        self._connect_blocks(cond_block, body_block)
        
        # Process body
        self.current_block = body_block
        self._process_statement(while_node.body)
        self._connect_blocks(body_block, cond_block)  # Loop back
        
        # Create exit block
        exit_block = self._new_block("LOOP EXIT")
        self._connect_blocks(cond_block, exit_block)
        
        self.current_block = exit_block

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
        
        # Create exit block
        exit_block = self._new_block("LOOP EXIT")
        self._connect_blocks(cond_block, exit_block)
        
        self.current_block = exit_block

    def _new_block(self, label=None):
        """Create a new basic block"""
        block_id = f"B{self.block_counter}"
        self.block_counter += 1
        self.cfg.add_node(block_id, label=label or "BLOCK")
        return block_id

    def _connect_blocks(self, from_block, to_block):
        """Connect two blocks in the CFG"""
        self.cfg.add_edge(from_block, to_block)

    def visualize(self, format="svg"):
        """Generate a visual representation of the CFG and return SVG content"""
        dot = Digraph(format=format)
        dot.attr('node', shape='box', style='rounded,filled', fillcolor='#e0f7fa', fontname='Courier')
        dot.attr('edge', arrowhead='vee')
        
        for node in self.cfg.nodes():
            label = self.cfg.nodes[node].get("label", node)
            dot.node(node, label=label)
        
        for src, dst in self.cfg.edges():
            dot.edge(src, dst)
        
        # Render to bytes and return SVG content
        svg_bytes = dot.pipe()
        return svg_bytes.decode('utf-8')
    