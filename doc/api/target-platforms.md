# Target platforms

Currently, Millfork supports creating disk- or tape-based programs for Commodore and Atari 8-bit computers, 
but it may be expanded to support other 6502-based platforms in the future.

## Supported platforms

The following platforms are currently supported:

* `c64` – Commodore 64

* `c16` – Commodore 16

* `plus4` – Commodore Plus/4

* `vic20` – Commodore VIC-20 without memory expansion

* `vic20_3k` – Commodore VIC-20 with 3K memory expansion

* `vic20_8k` – Commodore VIC-20 with 8K or 16K memory expansion

* `c128` – Commodore 128 in its native mode

* `pet` – Commodore PET

* `a8` – Atari 8-bit computers

The primary and most tested platform is Commodore 64.

Currently, all targets assume that the program will be loaded from disk or tape.
Cartridge targets are not yet available.

## Adding a custom platform

Every platform is defined in an `.ini` file with an appropriate name.

#### `[compilation]` section

* `arch` – CPU architecture. It defines which instructions are available. Available values: 

    * `nmos`
    
    * `strict` (= NMOS without illegal instructions) 
    
    * `ricoh` (= NMOS without decimal mode) 
    
    * `strictricoh`
    
    * `cmos` (= 65C02)

* `modules` – comma-separated list of modules that will be automatically imported

* other compilation options (they can be overridden using commandline options):

    * `emit_illegals` – whether the compiler should emit illegal instructions, default `false`
    
    * `emit_cmos` – whether the compiler should emit CMOS instructions, default is `true` on `cmos` and `false` elsewhere
    
    * `decimal_mode` – whether the compiler should emit decimal instructions, default is `false` on `ricoh` and `strictricoh` and `true` elsewhere
    
    * `ro_arrays` – whether the compiler should warn upon array writes, default is `false`
    
    * `prevent_jmp_indirect_bug` – whether the compiler should try to avoid the indirect JMP bug, default is `false` on `cmos` and `true` elsewhere

#### `[allocation]` section

* `main_org` – the address for the `main` function; all the other functions will be placed after it

* `zp_pointers` – either a list of comma separated zeropage addresses that can be used by the program as zeropage pointers, or `all` for all. Each value should be the address of the first of two free bytes in the zeropage.

* `himem_style` – not yet supported

* `himem_start` – the first address used for non-zeropage variables, or `after_code` if the variables should be allocated after the code

* `himem_end` – the last address available for non-zeropage variables

#### `[output]` section
 
* `style` – not yet supported

* `format` – output file format; a comma-separated list of tokens:

    * literal byte values
    
    * `startaddr` – little-endian 16-bit address of the first used byte of the compiled output
    
    * `endaddr` – little-endian 16-bit address of the last used byte of the compiled output
    
    * `allocated` – all used bytes
    
    * `<addr>:<addr>` - inclusive range of bytes
    
* `extension` – target file extension, with or without the dot