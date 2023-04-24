package me.heizi.jweust.beans

enum class Permissions {
    Default,
    HighersInTheRoom,
    Administrator,
}

//pub const EXE_IS_INSTANCE:bool = false;
//pub const EXE_PERMISSION:i8 = -1;
//pub const EXE_ICON_PATH:Option<&str> = Some("D:\\Downloads\\ic_ast_ugly.ico");
//pub const EXE_FILE_VERSION:&str = "0.0.0.9";
//pub const EXE_PRODUCT_VERSION:&str = "0.0.9";
//pub const EXE_INTERNAL_NAME:&str = "Android apk Sideload Tool From Heizi Flash Tools";
//pub const EXE_FILE_DESCRIPTION:&str = "线刷APK安装工具";
//pub const EXE_LEGAL_COPYRIGHT:&str = "Github/ElisaMin";
//pub const EXE_COMPANY_NAME:& str = "Heizi";
//
//// new
//pub const EXE_ARCH:&str = "x86_64";
//pub const EXE_PRODUCT_NAME:&str = "Android Package Sideload Tool";
@RustParsable.Prefix("EXE_")
data class ExeConfig(
    var isInstance: Boolean = true,
    var arch: String = "x86_64",
    @RustParsable.Type("i8")
    //EXE_PERMISSION
    @RustParsable.Name("PERMISSION")
    var permissions: Permissions = Permissions.Default,
    //EXE_ICON_PATH
    @RustParsable.Name("ICON_PATH")
    var icon: String? = null,
    var fileVersion: String = "",
    var productVersion: String = "",
    var productName: String = "",
    var internalName: String = "",
    var fileDescription: String = "",
    var legalCopyright: String = "",
    var companyName: String = ""
) : RustParsable {
    override fun parsingValueExtra(name: String): (() -> String) {
        return when (name) {
            "permissions" -> {{
                when (permissions) {
                    Permissions.HighersInTheRoom -> "2"
                    Permissions.Administrator -> "1"
                    else -> "3"
                } } }
            else -> error("")
        }
    }
}
