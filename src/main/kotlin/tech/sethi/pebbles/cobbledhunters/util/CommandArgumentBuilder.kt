package tech.sethi.pebbles.cobbledhunters.util

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.velocitypowered.api.command.CommandSource

fun literal(name: String): LiteralArgumentBuilder<CommandSource> {
    return LiteralArgumentBuilder.literal(name)
}

fun <T> argument(name: String?, type: ArgumentType<T>): RequiredArgumentBuilder<CommandSource, T> {
    return RequiredArgumentBuilder.argument(name, type)
}