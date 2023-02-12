class AST:
  node_id = 0

  def __init__(self, expr):
    self.name = ""
    self.children = []
    self.id = AST.node_id
    AST.node_id += 1
    if expr == None:
      return
    self.expr = expr

  def from_expr(expr, scope=[]):
    ast = AST(expr)
    className = expr.getClass().getSimpleName()
    #print(className)
    if className == 'ExprUnary':
      ast.name = expr.op.toString()
      if ast.name == "NOOP":
        ast.name = expr.type().toString()
        return AST.from_expr(expr.sub)
      ast.children.append(AST.from_expr(expr.sub))

    elif className == 'ExprList':
      ast.name = expr.op.toString()
      ast.children = list(map(lambda x: AST.from_expr(x), expr.args))

    elif className == 'ExprITE':
      ast.name = "ite"
      ast.children.append(AST.from_expr(expr.left))
      ast.children.append(AST.from_expr(expr.right))

    elif className == 'ExprQt':
      ast.name = expr.op.toString()

      scp = []
      for i in range(expr.decls.size()):
        scp.append(expr.decls.get(i).expr)
        ast.children.append(AST.from_expr(expr.decls.get(i).expr))

      ast.children.append(AST.from_expr(expr.sub))

    elif className == 'ExprBinary':
      ast.name = expr.op.toString()
      ast.children.append(AST.from_expr(expr.left))
      ast.children.append(AST.from_expr(expr.right))

    elif className == 'ExprVar':
      ast.name = "var/" + expr.type().toString()[1:][:-1]

    elif className == 'ExprConstant':
      ast.name = expr.toString()

    # Not sure about the difference between these
    elif className == 'SubsetSig':
      ast.name = expr.toString()

    elif className == 'PrimSig':
      ast.name = expr.toString()

    # Probably not the best way
    elif className == 'ExprCall':
      ast.name = "call"
      ast.children.append(AST.from_expr(expr.fun.getBody()))
      for child in list(map(lambda x: AST.from_expr(x), expr.args)):
        ast.children.append(child)
    
    elif className == 'Field':
      ast.name = 'field'
      ast.children.append(AST.from_expr(expr.decl().expr))
    
    else:
      print("Cannot build AST from ", end="'")
      print(className, end="' ")
      print(expr)
      exit(1)
    return ast

  def __str__(self):
    if len(self.children) == 0:
      return str(self.name)

    ret = "("
    ret += str(self.name)
    ret += " [ "
    for child in self.children:
      ret += child.__str__()
      ret += ", "
    ret = ret[:-2]
    ret += " ]"
    ret += ")"
    return ret

  def toApted(self):
    ret = "{" + str(self.name)
    for child in self.children:
      ret += child.toApted()
    ret += "}"
    return ret

