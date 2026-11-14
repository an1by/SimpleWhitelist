package net.aniby.simplewhitelist.mod.mixin;

import net.aniby.simplewhitelist.mod.VanillaModBridge;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class SimpleWhitelistMixinPlugin implements IMixinConfigPlugin {
    private static final String BRIDGE_INTERNAL = "net/aniby/simplewhitelist/mod/VanillaModBridge";

    @Override
    public void onLoad(String mixinPackage) {
        VanillaModBridge.INSTANCE.markMixinApplied();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (mixinClassName != null && mixinClassName.endsWith("PlayerListTargetMixin")) {
            transformPlayerList(targetClass);
        } else if (mixinClassName != null && mixinClassName.endsWith("CommandsTargetMixin")) {
            transformCommands(targetClass);
        }
    }

    public static void transformPlayerList(ClassNode classNode) {
        for (MethodNode method : classNode.methods) {
            if ("<init>".equals(method.name)) {
                injectInstanceInitCallback(classNode.name, method, "onPlayerListConstructed");
            } else if (isCanPlayerLoginMethod(method)) {
                injectCanPlayerLoginHead(method);
            }
        }
    }

    public static void transformCommands(ClassNode classNode) {
        for (MethodNode method : classNode.methods) {
            if ("<init>".equals(method.name)) {
                injectInstanceInitCallback(classNode.name, method, "onCommandsConstructed");
            }
        }
    }

    private static boolean isCanPlayerLoginMethod(MethodNode method) {
        if ((method.access & Opcodes.ACC_STATIC) != 0 || (method.access & Opcodes.ACC_ABSTRACT) != 0) {
            return false;
        }
        Type[] args = Type.getArgumentTypes(method.desc);
        Type ret = Type.getReturnType(method.desc);
        return args.length == 2
                && "java/net/SocketAddress".equals(args[0].getInternalName())
                && args[1].getSort() == Type.OBJECT
                && ret.getSort() == Type.OBJECT
                && !"java/lang/String".equals(ret.getInternalName());
    }

    private static void injectInstanceInitCallback(String ownerInternalName, MethodNode method, String bridgeMethodName) {
        if (delegatesToSameClassConstructor(ownerInternalName, method)) {
            return;
        }
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == Opcodes.RETURN) {
                InsnList hook = new InsnList();
                hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
                hook.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        BRIDGE_INTERNAL,
                        bridgeMethodName,
                        "(Ljava/lang/Object;)V",
                        false
                ));
                method.instructions.insertBefore(insn, hook);
            }
        }
        method.maxStack = Math.max(method.maxStack, 2);
    }

    private static boolean delegatesToSameClassConstructor(String ownerInternalName, MethodNode method) {
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == Opcodes.INVOKESPECIAL && insn instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) insn;
                if ("<init>".equals(call.name)) {
                    return ownerInternalName.equals(call.owner);
                }
            }
        }
        return false;
    }

    private static void injectCanPlayerLoginHead(MethodNode method) {
        Type returnType = Type.getReturnType(method.desc);
        LabelNode continueLabel = new LabelNode();

        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 2));
        hook.add(new LdcInsnNode(returnType.getClassName()));
        hook.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                BRIDGE_INTERNAL,
                "checkCanPlayerLogin",
                "(Ljava/lang/Object;Ljava/net/SocketAddress;Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;",
                false
        ));
        hook.add(new InsnNode(Opcodes.DUP));
        hook.add(new JumpInsnNode(Opcodes.IFNULL, continueLabel));
        hook.add(new TypeInsnNode(Opcodes.CHECKCAST, returnType.getInternalName()));
        hook.add(new InsnNode(Opcodes.ARETURN));
        hook.add(continueLabel);
        hook.add(new FrameNode(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/Object"}));
        hook.add(new InsnNode(Opcodes.POP));

        method.instructions.insert(hook);
        method.maxStack = Math.max(method.maxStack, 5);
    }
}
