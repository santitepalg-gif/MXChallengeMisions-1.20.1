package com.libromx.server;

import com.libromx.data.BookData;
import com.libromx.data.Entry;
import com.libromx.net.Net;
import com.libromx.util.TextFmt;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Locale;

public class BookCommands {
    private static boolean isOp(CommandSourceStack s) {
        return s.hasPermission(2);
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("libro")
                .executes(c -> {
                    Net.sync(c.getSource().getPlayerOrException(), true);
                    return 1;
                });

        // /libro mision agregar|quitar|recompensa
        root.then(Commands.literal("mision").requires(BookCommands::isOp)
                .then(Commands.literal("agregar")
                        .then(Commands.argument("tipo", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Actions.TYPES, b))
                                .then(Commands.argument("objetivo", ResourceLocationArgument.id())
                                        .then(Commands.argument("titulo", StringArgumentType.string())
                                                .then(Commands.argument("descripcion", StringArgumentType.greedyString())
                                                        .executes(BookCommands::addQuest))))))
                .then(Commands.literal("quitar")
                        .then(Commands.argument("indice", IntegerArgumentType.integer(1))
                                .executes(c -> remove(c, 0))))
                .then(Commands.literal("recompensa")
                        .then(Commands.argument("indice", IntegerArgumentType.integer(1))
                                .then(Commands.argument("item", ResourceLocationArgument.id())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggestResource(ForgeRegistries.ITEMS.getKeys(), b))
                                        .executes(c -> reward(c, 1))
                                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 9999))
                                                .executes(c -> reward(c, IntegerArgumentType.getInteger(c, "cantidad"))))))));

        // /libro notificacion enviar|quitar
        root.then(Commands.literal("notificacion").requires(BookCommands::isOp)
                .then(Commands.literal("enviar")
                        .then(Commands.argument("texto", StringArgumentType.greedyString())
                                .executes(BookCommands::addNotice)))
                .then(Commands.literal("quitar")
                        .then(Commands.argument("indice", IntegerArgumentType.integer(1))
                                .executes(c -> remove(c, 1)))));

        // /libro dificultad agregar|establecer|quitar
        root.then(Commands.literal("dificultad").requires(BookCommands::isOp)
                .then(Commands.literal("agregar")
                        .then(Commands.argument("base", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Actions.BASES, b))
                                .then(Commands.argument("titulo", StringArgumentType.string())
                                        .then(Commands.argument("descripcion", StringArgumentType.greedyString())
                                                .executes(BookCommands::addDifficulty)))))
                .then(Commands.literal("establecer")
                        .then(Commands.argument("indice", IntegerArgumentType.integer(1))
                                .executes(c -> {
                                    int i = IntegerArgumentType.getInteger(c, "indice") - 1;
                                    if (!Actions.applyDifficulty(c.getSource().getServer(), i)) {
                                        c.getSource().sendFailure(Component.literal("Indice invalido."));
                                        return 0;
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("quitar")
                        .then(Commands.argument("indice", IntegerArgumentType.integer(1))
                                .executes(c -> remove(c, 2)))));

        // /libro lista <mision|notificacion|dificultad>
        root.then(Commands.literal("lista").requires(BookCommands::isOp)
                .then(listNode("mision", 0))
                .then(listNode("notificacion", 1))
                .then(listNode("dificultad", 2)));

        // /libro reiniciar [todos]
        root.then(Commands.literal("reiniciar").requires(BookCommands::isOp)
                .executes(c -> {
                    Progress.reset(c.getSource().getPlayerOrException());
                    c.getSource().sendSuccess(() -> Component.literal("Tu progreso de misiones se reinicio."), false);
                    return 1;
                })
                .then(Commands.literal("todos").executes(c -> {
                    Progress.resetAll(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.literal("Progreso de todos reiniciado."), true);
                    return 1;
                })));

        d.register(root);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> listNode(String name, int book) {
        return Commands.literal(name).executes(c -> {
            List<Entry> l = BookData.server().list(book);
            if (l.isEmpty()) {
                c.getSource().sendSuccess(() -> Component.literal("(vacio)"), false);
            }
            for (int i = 0; i < l.size(); i++) {
                Entry e = l.get(i);
                String extra = "";
                if (book == 0) {
                    extra = " [" + e.type + ", recompensas: " + e.rewards.size() + "]";
                }
                String line = (i + 1) + ". " + TextFmt.strip(e.title) + extra;
                c.getSource().sendSuccess(() -> Component.literal(line), false);
            }
            return l.size();
        });
    }

    private static int addQuest(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        String tipo = StringArgumentType.getString(c, "tipo").toLowerCase(Locale.ROOT);
        if (!Actions.TYPES.contains(tipo)) {
            c.getSource().sendFailure(Component.literal("Tipo invalido. Usa kill, collect o craft."));
            return 0;
        }
        ResourceLocation obj = ResourceLocationArgument.getId(c, "objetivo");
        Entry e = new Entry();
        e.title = StringArgumentType.getString(c, "titulo");
        e.desc = StringArgumentType.getString(c, "descripcion");
        e.type = tipo;
        e.targets.add(obj.toString());
        if (!Actions.add(c.getSource().getServer(), 0, e)) {
            c.getSource().sendFailure(Component.literal("No se pudo agregar (objetivo invalido o libro lleno)."));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.literal("Mision agregada."), true);
        return 1;
    }

    private static int addNotice(CommandContext<CommandSourceStack> c) {
        Entry e = new Entry();
        e.title = StringArgumentType.getString(c, "texto");
        if (!Actions.add(c.getSource().getServer(), 1, e)) {
            c.getSource().sendFailure(Component.literal("No se pudo enviar."));
            return 0;
        }
        return 1;
    }

    private static int addDifficulty(CommandContext<CommandSourceStack> c) {
        Entry e = new Entry();
        e.base = StringArgumentType.getString(c, "base");
        e.title = StringArgumentType.getString(c, "titulo");
        e.desc = StringArgumentType.getString(c, "descripcion");
        if (!Actions.add(c.getSource().getServer(), 2, e)) {
            c.getSource().sendFailure(Component.literal("No se pudo agregar."));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.literal("Dificultad agregada."), true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> c, int book) {
        int i = IntegerArgumentType.getInteger(c, "indice") - 1;
        if (!Actions.remove(c.getSource().getServer(), book, i)) {
            c.getSource().sendFailure(Component.literal("Indice invalido."));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.literal("Quitado."), true);
        return 1;
    }

    private static int reward(CommandContext<CommandSourceStack> c, int count) throws CommandSyntaxException {
        int i = IntegerArgumentType.getInteger(c, "indice") - 1;
        ResourceLocation item = ResourceLocationArgument.getId(c, "item");
        if (!Actions.addReward(c.getSource().getServer(), i, item, count)) {
            c.getSource().sendFailure(Component.literal("Indice o item invalido (maximo 18 recompensas)."));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.literal("Recompensa agregada."), true);
        return 1;
    }
}
