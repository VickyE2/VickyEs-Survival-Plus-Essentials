# VickyE's Survival Plus Essentials - Platform Antagnostic Verion

---
Uhhh so ig this is my document. I'll try documenting every thing (only in VSPE module) that this project offers...
___
### [VSPEPlatformPlugin.java](VSPE/src/main/java/org/vicky/vspe/platform/VSPEPlatformPlugin.java):
> This is the platform antagnostic interface the other platforms should implement on ther main class to allow the other classes in this project to access utilities and other data variables.
> <br> It is worth it to note that the #isNative() just specifies if the platform its being used on allows for custom item registering.

### [PlatformStructureManager.java](VSPE/src/main/java/org/vicky/vspe/platform/PlatformStructureManager.java)
> This is basiclly where the dimensions call "HEY UNC! I fnished my thinking i should place...some of "that" here so give me it..."
> <br> If not implemented....The whole system goes brr...I should prolly make a default soon

### [PlatformDimensionFactory.java](VSPE/src/main/java/org/vicky/vspe/platform/PlatformDimensionFactory.java)
> This is as it sounds the dimension maker. I makes dimensions from [DimensionDescriptor](VSPE/src/main/java/org/vicky/vspe/platform/systems/dimension/DimensionDescriptor.java)
> <br> The said descriptor holds a LOT of informtion about said dimension...I haven't implemented mob spawning so something about that might come later...

### [PlatformBiomeFactory.java](VSPE/src/main/java/org/vicky/vspe/platform/PlatformBiomeFactory.java)
> Like it sounds I found a way to make "producing" minecraft biomes antagnostic by reading NMS, Forge and Fabric biome creation each being HELLA similar so this factory uses
> [BiomeParameters](VSPE/src/main/kotlin/org/vicky/vspe/platform/systems/dimension/vspeChunkGenerator/Biome.kt#L243) which is then passed to said factory on #createBiome casted to inline type
___

## Procedural Structure Generation

Well whats showing off that "I can code" Withouth making EVERYTHING with code...including structures...ofc if you read my commit cycle you'd know this was a pain to achieve but hehe it worked
<br> Anyways off to the parts...

### [SpiralUtil.java](VSPE/src/main/java/org/vicky/vspe/platform/systems/dimension/StructureUtils/SpiralUtil.java)
> This was inspired by hehe whats the name again... leme serch that up rq...YES EZEDITS
> So basically we have a list of control points that give us a bezier path [BezierCurve](VSPE/src/main/java/org/vicky/vspe/platform/systems/dimension/StructureUtils/BezierCurve.java) that can 
> then be decorated by a set of defaults [DefaultDecorators](VSPE/src/main/java/org/vicky/vspe/platform/systems/dimension/StructureUtils/SpiralUtil.java#L714)

### [CorePointsFactory.java](VSPE/src/main/java/org/vicky/vspe/platform/systems/dimension/StructureUtils/CorePointsFactory.java)
> This is a class that can make a path or path shape from a set of parameters. So its allows for better randomness especilly so structures don't js look TOO static

### [ProceduralStructureGenerator.java](VSPE/src/main/java/org/vicky/vspe/platform/systems/dimension/StructureUtils/ProceduralStructureGenerator.java)
> This is an abstract class that must be implemented to produce a GenerationResult structure. This is done with maths and a LOT of prayers lmao
> Most can be found directly in [\"This Folder\"](VSPE/src/main/java/org/vicky/vspe/platform/systems/dimension/StructureUtils/Generators)