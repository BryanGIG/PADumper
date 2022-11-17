# PAD (Process Android Dumper)
This dumper is made for il2cpp game but you can use it in any app you want

## How To Use
- Run the process
- Open PADumper
- Put process name manually or you can click `Select Apps` to select running apps
- Put the ELF Name or you can leave it with default name `libil2cpp.so`

- [**Optional**] UnCheck `Check flag address` if you want to skip check address permission (**r-xp**)
- [**Optional**] Check `Fix ELF` if you want fix the ELF
- [**Optional**] Check `global-metadata.dat` if you want dump unity metadata from memory
- Dump and wait until finish
- Result will be in `/sdcard/PADumper/[Process]/[startAddress-endAddress-file]`

## Credits
- [**libsu**](https://github.com/topjohnwu/libsu)
- [**SoFixer**](https://github.com/F8LEFT/SoFixer)
