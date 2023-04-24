package me.heizi.jweust.beans

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import java.io.Serializable

enum class Permissions:Serializable {
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
    @get:Input
    var isInstance: Boolean = true,
    @get:Input
    var arch: String = "x86_64",
    @get:Input
    @RustParsable.Type("i8")
    @RustParsable.Name("PERMISSION")
    var permissions: Permissions = Permissions.Default,
    @get:Input
    @get:Optional
    @RustParsable.Name("ICON_PATH")
    var icon: String? = null,
    @get:Input
    var fileVersion: String = "",
    @get:Input
    var productVersion: String = "",
    @get:Input
    var productName: String = "",
    @get:Input
    var internalName: String = "",
    @get:Input
    var fileDescription: String = "",
    @get:Input
    var legalCopyright: String = "",
    @get:Input
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
