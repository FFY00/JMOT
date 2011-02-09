package net.contra.obfuscator.trans;

import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.generic.*;
import net.contra.obfuscator.Settings;
import net.contra.obfuscator.util.BCELMethods;
import net.contra.obfuscator.util.JarLoader;
import net.contra.obfuscator.util.LogHandler;


public class IntegerComplicator implements ITransformer {
    private final LogHandler Logger = new LogHandler("AttributeObfuscator");
    private String Location = "";
    private JarLoader LoadedJar;

    public IntegerComplicator(String loc) {
        Location = loc;
    }

    public void Load() {
        LoadedJar = new JarLoader(Location);
    }

    public void Transform() {
        for (ClassGen cg : LoadedJar.ClassEntries.values()) {
            for (Method method : cg.getMethods()) {
                MethodGen mg = new MethodGen(method, cg.getClassName(), cg.getConstantPool());
                InstructionList list = mg.getInstructionList();
                if (list == null) continue;
                Logger.Log("Complicating Constant Integers -> Class: " + cg.getClassName() + " Method: " + method.getName());
                InstructionHandle[] handles = list.getInstructionHandles();
                for (InstructionHandle handle : handles) {
                    if (handle.getPosition() >= handles.length - 1) continue;
                    if (Settings.ObfuscationLevel.getLevel() <= ObfuscationType.Normal.getLevel()) {
                        //If we have it on normal or light, we won't obfuscate boolean values :)
                        if (BCELMethods.isFieldInvoke(handle.getNext().getInstruction())) {
                            String sig = BCELMethods.getFieldInvokeSignature(handle.getNext().getInstruction(), cg.getConstantPool());
                            if (sig.trim().startsWith("Z")) continue;
                        }
                        if (handle.getNext().getInstruction() instanceof IRETURN) {
                            if (mg.getSignature().trim().endsWith("Z")) continue;
                        }
                    }
                    if (handle.getInstruction() instanceof ICONST
                            || handle.getInstruction() instanceof BIPUSH
                            || handle.getInstruction() instanceof SIPUSH) {
                        InstructionList nlist = new InstructionList();
                        for (int i = 0; i < Settings.Iterations; i++) {
                            nlist.append(new ICONST(1));
                            nlist.append(new IMUL());
                            if (Settings.ObfuscationLevel.getLevel() > ObfuscationType.Normal.getLevel()) {
                                nlist.append(new ICONST(1));
                                nlist.append(new IDIV());
                            }
                        }
                        list.append(handle, nlist);
                    }
                    if (handle.getInstruction() instanceof LDC
                            && handle.getNext().getInstruction() instanceof IASTORE
                            && Settings.ObfuscationLevel.getLevel() >= ObfuscationType.Normal.getLevel()) {
                        //If we have it on normal, heavy, or insane it will obfuscate
                        InstructionList nlist = new InstructionList();
                        for (int i = 0; i < Settings.Iterations; i++) {
                            nlist.append(new ICONST(1));
                            nlist.append(new IMUL());
                            if (Settings.ObfuscationLevel.getLevel() > ObfuscationType.Normal.getLevel()) {
                                nlist.append(new ICONST(1));
                                nlist.append(new IDIV());
                            }
                        }
                        list.append(handle, nlist);
                    }
                }
                list.setPositions();
                mg.setInstructionList(list);
                mg.setMaxLocals();
                mg.setMaxStack();
                cg.replaceMethod(method, mg.getMethod());
            }
        }
    }

    public void Dump() {
        LoadedJar.Save(Location.replace(".jar", "-new.jar"));
    }
}

