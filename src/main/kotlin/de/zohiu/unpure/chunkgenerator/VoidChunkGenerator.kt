package de.zohiu.unpure.chunkgenerator

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Biome
import org.bukkit.generator.BiomeProvider
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.ChunkGenerator
import org.bukkit.generator.ChunkGenerator.ChunkData
import org.bukkit.generator.WorldInfo
import java.util.Random

class VoidChunkGenerator : ChunkGenerator() {

    override fun getDefaultPopulators(world: World): List<BlockPopulator> = emptyList()

    override fun generateNoise(worldInfo: WorldInfo, random: Random, chunkX: Int, chunkZ: Int, chunkData: ChunkData) {
        // No need to generate noise, we want an empty world
    }

    override fun generateSurface(worldInfo: WorldInfo, random: Random, chunkX: Int, chunkZ: Int, chunkData: ChunkData) {
        // No need to generate surface, we want an empty world
    }

    override fun generateBedrock(worldInfo: WorldInfo, random: Random, chunkX: Int, chunkZ: Int, chunkData: ChunkData) {
        // No need to generate bedrock, we want an empty world
    }

    override fun generateCaves(worldInfo: WorldInfo, random: Random, chunkX: Int, chunkZ: Int, chunkData: ChunkData) {
        // No need to generate caves, we want an empty world
    }

    override fun getDefaultBiomeProvider(worldInfo: WorldInfo): BiomeProvider = VoidBiomeProvider()

    override fun canSpawn(world: World, x: Int, z: Int): Boolean = true

    override fun getFixedSpawnLocation(world: World, random: Random): Location {
        return Location(
            world, 0.0, 64.0, 0.0
        )
    }
}

class VoidBiomeProvider : BiomeProvider() {
    override fun getBiome(worldInfo: WorldInfo, x: Int, y: Int, z: Int): Biome = Biome.THE_VOID

    override fun getBiomes(worldInfo: WorldInfo): List<Biome> = listOf(Biome.THE_VOID);
}
