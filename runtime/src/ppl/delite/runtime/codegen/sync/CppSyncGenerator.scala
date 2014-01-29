package ppl.delite.runtime.codegen.sync


import ppl.delite.runtime.graph.ops._
import collection.mutable.ArrayBuffer
import ppl.delite.runtime.graph.targets.{OS, Targets}
import ppl.delite.runtime.codegen._
import ppl.delite.runtime.scheduler.OpHelper._
import ppl.delite.runtime.graph.targets.Targets._

trait CppToScalaSync extends SyncGenerator with CppExecutableGenerator with JNIFuncs {

  private val syncList = new ArrayBuffer[Send]
  
  override protected def receiveData(s: ReceiveData) {
    getHostTarget(scheduledTarget(s.sender.from)) match {
      case Targets.Scala => writeGetter(s.sender.from, s.sender.sym, s.to, false)
      case _ => super.receiveData(s)
    }
  }

  override protected def receiveView(s: ReceiveView) {
    getHostTarget(scheduledTarget(s.sender.from)) match {
      case Targets.Scala => writeGetter(s.sender.from, s.sender.sym, s.to, true)
      case _ => super.receiveView(s)
    }
  }

  override protected def awaitSignal(s: Await) {
    getHostTarget(scheduledTarget(s.sender.from)) match {
      case Targets.Scala => writeAwaiter(s.sender.from)
      case _ => super.awaitSignal(s)
    }
  }

  override protected def receiveUpdate(s: ReceiveUpdate) {
    getHostTarget(scheduledTarget(s.sender.from)) match {
      case Targets.Scala => 
        s.sender.from.mutableInputsCondition.get(s.sender.sym) match {
          case Some(lst) => 
            out.append("if(")
            out.append(lst.map(c => c._1.id.split('_').head + "_cond=="+c._2).mkString("&&"))
            out.append(") {\n")
            writeAwaiter(s.sender.from); writeRecvUpdater(s.sender.from, s.sender.sym); 
            out.append("}\n")
          case _ => 
            writeAwaiter(s.sender.from); writeRecvUpdater(s.sender.from, s.sender.sym);
        } 
      case _ => super.receiveUpdate(s)
    }
  }

  override protected def sendData(s: SendData) {
    if (s.receivers.map(_.to).filter(r => getHostTarget(scheduledTarget(r)) == Targets.Scala).nonEmpty) {
      writeSetter(s.from, s.sym, false)
      syncList += s
    }
    //else {
    super.sendData(s)  // TODO: always call super? (when multiple receivers have different host types)
    //}
  }

  override protected def sendView(s: SendView) {
    if (s.receivers.map(_.to).filter(r => getHostTarget(scheduledTarget(r)) == Targets.Scala).nonEmpty) {
      writeSetter(s.from, s.sym, true)
      syncList += s
    }
    //else {
      super.sendView(s)  // TODO: always call super? (when multiple receivers have different host types)
    //}
  }

  override protected def sendSignal(s: Notify) {
    if (s.receivers.map(_.to).filter(r => getHostTarget(scheduledTarget(r)) == Targets.Scala).nonEmpty) {
      writeNotifier(s.from)
      syncList += s
    }
    //else {
      super.sendSignal(s)  // TODO: always call super? (when multiple receivers have different host types)
    //}
  }

  override protected def sendUpdate(s: SendUpdate) {
    if (s.receivers.map(_.to).filter(r => getHostTarget(scheduledTarget(r)) == Targets.Scala).nonEmpty) {
      s.from.mutableInputsCondition.get(s.sym) match {
        case Some(lst) => 
          out.append("if(")
          out.append(lst.map(c => c._1.id.split('_').head + "_cond=="+c._2).mkString("&&"))
          out.append(") {\n")
          writeSendUpdater(s.from, s.sym)
          writeNotifier(s.from)
          out.append("}\n")
        case _ => 
          writeSendUpdater(s.from, s.sym)
          writeNotifier(s.from)
      }      
      syncList += s
    }
    //else {
      super.sendUpdate(s)  // TODO: always call super? (when multiple receivers have different host types)
    //}
  }

  private def writeGetter(dep: DeliteOP, sym: String, to: DeliteOP, view: Boolean) {
    out.append(getJNIType(dep.outputType(sym)))
    out.append(' ')
    out.append(getSymCPU(sym))
    out.append(" = ")
    out.append("env")
    out.append(location)
    out.append("->CallStatic")
    out.append(getJNIFuncType(dep.outputType(sym)))
    out.append("Method(cls")
    out.append(dep.scheduledResource)
    out.append(",env")
    out.append(location)
    out.append("->GetStaticMethodID(cls")
    out.append(dep.scheduledResource)
    out.append(",\"get")
    out.append(location)
    out.append('_')
    out.append(getSym(dep,sym))
    out.append("\",\"()")
    out.append(getJNIOutputType(dep.outputType(Targets.Scala,sym)))
    out.append("\"));\n")
    val ref = "" //if (isPrimitiveType(dep.outputType(sym))) "" else "*"
    val devType = CppExecutableGenerator.typesMap(Targets.Cpp)(sym)
    if (view)
      out.append("%s %s%s = recvViewCPPfromJVM_%s(env%s,%s);\n".format(devType,ref,getSymHost(dep,sym),mangledName(devType),location,getSymCPU(sym)))
    else if(isPrimitiveType(dep.outputType(sym)))
      out.append("%s %s = (%s)%s;\n".format(devType,getSymHost(dep,sym),devType,getSymCPU(sym)))
    else
      out.append("%s %s%s = recvCPPfromJVM_%s(env%s,%s);\n".format(devType,ref,getSymHost(dep,sym),mangledName(devType),location,getSymCPU(sym)))
  }

  private def writeAwaiter(dep: DeliteOP) {
    out.append("env")
    out.append(location)
    out.append("->CallStaticVoid")
    out.append("Method(cls")
    out.append(dep.scheduledResource)
    out.append(",env")
    out.append(location)
    out.append("->GetStaticMethodID(cls")
    out.append(dep.scheduledResource)
    out.append(",\"get")
    out.append(location)
    out.append('_')
    out.append(getOpSym(dep))
    out.append("\",\"()V")
    out.append("\"));\n")
  }

  private def writeRecvUpdater(dep: DeliteOP, sym:String) {
    val devType = CppExecutableGenerator.typesMap(Targets.Cpp)(sym)
    println(dep.id + ":" + sym)
    println(dep.getInputTypesMap.toString)
    assert(!isPrimitiveType(dep.inputType(sym)))
    out.append("recvUpdateCPPfromJVM_%s(env%s,%s,%s);\n".format(mangledName(devType),location,getSymCPU(sym),getSymHost(dep,sym)))
  }

  private def writeSetter(op: DeliteOP, sym: String, view: Boolean) {
    val devType = CppExecutableGenerator.typesMap(Targets.Cpp)(sym)
    if (view)
      out.append("%s %s = sendViewCPPtoJVM_%s(env%s,%s);\n".format(getJNIType(op.outputType(sym)),getSymCPU(sym),mangledName(devType),location,getSymHost(op,sym)))
    else if(isPrimitiveType(op.outputType(sym)))
      out.append("%s %s = (%s)%s;\n".format(getJNIType(op.outputType(sym)),getSymCPU(sym),getJNIType(op.outputType(sym)),getSymHost(op,sym)))
    else
      out.append("%s %s = sendCPPtoJVM_%s(env%s,%s);\n".format(getJNIType(op.outputType(sym)),getSymCPU(sym),mangledName(devType),location,getSymHost(op,sym)))

    out.append("env")
    out.append(location)
    out.append("->CallStaticVoidMethod(cls")
    out.append(location)
    out.append(",env")
    out.append(location)
    out.append("->GetStaticMethodID(cls")
    out.append(location)
    out.append(",\"set_")
    out.append(getSym(op,sym))
    out.append("\",\"(")
    out.append(getJNIArgType(op.outputType(sym)))
    out.append(")V\"),")
    out.append(getSymCPU(sym))
    out.append(");\n")
  }

  private def writeNotifier(op: DeliteOP) {
    out.append("env")
    out.append(location)
    out.append("->CallStaticVoidMethod(cls")
    out.append(location)
    out.append(",env")
    out.append(location)
    out.append("->GetStaticMethodID(cls")
    out.append(location)
    out.append(",\"set_")
    out.append(getOpSym(op))
    out.append("\",\"(")
    out.append(getJNIArgType("Unit"))
    out.append(")V\"),")
    out.append("boxedUnit")
    out.append(");\n")
  }

  private def writeSendUpdater(op: DeliteOP, sym: String) {
    val devType = CppExecutableGenerator.typesMap(Targets.Cpp)(sym)
    assert(!isPrimitiveType(op.inputType(sym)))
    out.append("sendUpdateCPPtoJVM_%s(env%s,%s,%s);\n".format(mangledName(devType),location,getSymCPU(sym),getSymHost(op,sym)))
  }

  override protected def writeSyncObject() {
    //if (syncList.nonEmpty) {
      syncObjectGenerator(syncList, Targets.Scala).makeSyncObjects
    //}
    super.writeSyncObject()
  }
}

trait CppToCppSync extends SyncGenerator with CppExecutableGenerator with JNIFuncs {

  private val syncList = new ArrayBuffer[Send]

  override protected def receiveData(s: ReceiveData) {
    getHostTarget(scheduledTarget(s.sender.from)) match {
      case Targets.Cpp => writeGetter(s.sender.from, s.sender.sym, s.to)
      case _ => super.receiveData(s)
    }
  }

  override protected def sendData(s: SendData) {
    if (s.receivers.map(_.to).filter(r => getHostTarget(scheduledTarget(r)) == Targets.Cpp).nonEmpty) {
      writeSetter(s.from, s.sym)
      syncList += s
    }
    //else {
    super.sendData(s)  // TODO: always call super? (when multiple receivers have different host types)
    //}
  }

  override protected def receiveView(s: ReceiveView) {
    getHostTarget(scheduledTarget(s.sender.from)) match {
      case Targets.Cpp => writeGetter(s.sender.from, s.sender.sym, s.to)
      case _ => super.receiveView(s)
    }
  }

  override protected def awaitSignal(s: Await) {
    getHostTarget(scheduledTarget(s.sender.from)) match {
      case Targets.Cpp => writeAwaiter(s.sender.from)
      case _ => super.awaitSignal(s)
    }
  }

  override protected def sendView(s: SendView) {
    if (s.receivers.map(_.to).filter(r => getHostTarget(scheduledTarget(r)) == Targets.Cpp).nonEmpty) {
      writeSetter(s.from, s.sym)
      syncList += s
    }
    //else {
      super.sendView(s)  // TODO: always call super? (when multiple receivers have different host types)
    //}
  }

  override protected def sendSignal(s: Notify) {
    if (s.receivers.map(_.to).filter(r => getHostTarget(scheduledTarget(r)) == Targets.Cpp).nonEmpty) {
      writeNotifier(s.from)
      syncList += s
    }
    //else {
      super.sendSignal(s)  // TODO: always call super? (when multiple receivers have different host types)
    //}
  }

  private def writeGetter(dep: DeliteOP, sym: String, to: DeliteOP) {
    val tpe = CppExecutableGenerator.typesMap(Targets.Cpp)(sym)
    out.append(tpe)
    out.append(' ')
    //if (!isPrimitiveType(dep.outputType(sym))) out.append(" *")
    if (tpe.startsWith("MultiLoopHeader")) out.append(" *")
    out.append(getSymHost(dep, sym))
    out.append(" = ")
    out.append("get")
    out.append(location)
    out.append('_')
    out.append(getSym(dep, sym))
    out.append("();\n")
  }

  private def writeAwaiter(dep: DeliteOP) {
    out.append("get")
    out.append(location)
    out.append('_')
    out.append(getOpSym(dep))
    out.append("();\n")
  }

  private def writeSetter(op: DeliteOP, sym: String) {
    out.append("set_")
    out.append(getSym(op, sym))
    out.append('(')
    out.append(getSymHost(op, sym))
    out.append(')')
    out.append(";\n")
  }

  private def writeNotifier(op: DeliteOP) {
    out.append("set_")
    out.append(getOpSym(op))
    out.append("();\n")
  }

  override protected def writeSyncObject() {
    //if (syncList.nonEmpty) {
      syncObjectGenerator(syncList, Targets.Cpp).makeSyncObjects
    //}
    super.writeSyncObject()
  }

}

trait CppSyncGenerator extends CppToScalaSync with CppToCppSync {

  override protected def makeNestedFunction(op: DeliteOP) = op match {
    //case r: Receive if (getHostTarget(scheduledTarget(r.sender.from)) == Targets.Scala) => addSync(r)
    //case s: Send if (s.receivers.map(_.to).filter(r => getHostTarget(scheduledTarget(r)) == Targets.Scala).nonEmpty) => addSync(s)
    case s: Sync => addSync(s) //TODO: if sync companion also Scala
    case Free(o,items) => 
      val freeItems = items.filter(i => !isPrimitiveType(i._1.outputType(i._2)))
      if(freeItems.size > 0) {
        for(item <- freeItems) {
          val tpe = CppExecutableGenerator.typesMap(Targets.Cpp)(item._2)
          if(tpe.startsWith("std::shared_ptr"))
            out.append("" + getSymHost(item._1,item._2) + ".reset();\n")
          else if(tpe != "void" && item._1.scheduledResource==location) // for multiloop header (only the producer deletes it)
            out.append("delete " + getSymHost(item._1,item._2) + ";\n")
        }
      }
      //println("[warning] freeing " + m.items.map(_._2).mkString(",") + " is not inserted.")
    case _ => super.makeNestedFunction(op)
  }
}

trait JNIFuncs {
    protected def getJNIType(scalaType: String): String = {
    scalaType match {
      case "Unit" => "void"
      case "Int" => "jint"
      case "Long" => "jlong"
      case "Float" => "jfloat"
      case "Double" => "jdouble"
      case "Boolean" => "jboolean"
      case "Short" => "jshort"
      case "Char" => "jchar"
      case "Byte" => "jbyte"
      //case r if r.startsWith("generated.scala.Ref[") => getJNIType(r.slice(20,r.length-1))
      case _ => "jobject"//all other types are objects
    }
  }

  protected def getJNIArgType(scalaType: String): String = {
    scalaType match {
      case "Unit" => "Lscala/runtime/BoxedUnit;"
      case "Int" => "I"
      case "Long" => "J"
      case "Float" => "F"
      case "Double" => "D"
      case "Boolean" => "Z"
      case "Short" => "S"
      case "Char" => "C"
      case "Byte" => "B"
      //case r if r.startsWith("generated.scala.Ref[") => getJNIArgType(r.slice(20,r.length-1))
      case array if array.startsWith("Array[") => "[" + getJNIArgType(array.slice(6,array.length-1))
      case _ => { //all other types are objects
        var objectType = scalaType.replace('.','/')
        if (objectType.indexOf('[') != -1) objectType = objectType.substring(0, objectType.indexOf('[')) //erasure
        "L"+objectType+";" //'L' + fully qualified type + ';'
      }
    }
  }

  protected def getJNIOutputType(scalaType: String): String = {
    scalaType match {
      case "Unit" => "V"
      case "Int" => "I"
      case "Long" => "J"
      case "Float" => "F"
      case "Double" => "D"
      case "Boolean" => "Z"
      case "Short" => "S"
      case "Char" => "C"
      case "Byte" => "B"
      //case r if r.startsWith("generated.scala.Ref[") => getJNIOutputType(r.slice(20,r.length-1))
      case array if array.startsWith("Array[") => "[" + getJNIOutputType(array.slice(6,array.length-1))
      case _ => { //all other types are objects
        var objectType = scalaType.replace('.','/')
        if (objectType.indexOf('[') != -1) objectType = objectType.substring(0, objectType.indexOf('[')) //erasure
        "L"+objectType+";" //'L' + fully qualified type + ';'
      }
    }
  }

  protected def getJNIFuncType(scalaType: String): String = scalaType match {
    case "Unit" => "Void"
    case "Int" => "Int"
    case "Long" => "Long"
    case "Float" => "Float"
    case "Double" => "Double"
    case "Boolean" => "Boolean"
    case "Short" => "Short"
    case "Char" => "Char"
    case "Byte" => "Byte"
    //case r if r.startsWith("generated.scala.Ref[") => getJNIFuncType(r.slice(20,r.length-1))
    case _ => "Object"//all other types are objects
  }

  protected def getCPrimitiveType(scalaType: String): String = scalaType match {
    case "Unit" => "void"
    case "Int" => "int"
    case "Long" => "long"
    case "Float" => "float"
    case "Double" => "double"
    case "Boolean" => "bool"
    case "Short" => "short"
    case "Char" => "char"
    case "Byte" => "char"
    //case r if r.startsWith("generated.scala.Ref[") => getCPrimitiveType(r.slice(20,r.length-1))
    case other => error(other + " is not a primitive type")
  }
}
