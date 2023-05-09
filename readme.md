# Jweust
a gradle plugin to generating exe files for windows using rust and binding jvm.dll
> gpt told me this , so im trying to make it work  
## 整理脑子
### config
version of json
```js
let executable_config = {
    name:"sideload-install-wizard",
    "INCLUDE_JAR": false,
    "application_type": ()=> WindowsService ({
        "description": "",
        ...
    }),// or console , Application(NoConsole or by "-c")
    "workdir": ".",
    "log": {
        "error": {
            "path": "error.log",
            "isOverwrite": false
        },
        "stdout": null,
    },
    exe:{
        "Instance": true,
        "arch":"x86_64",
        "Premissions":Administrator, // or DefaultUser ,
        "Icon":"icon.ico",
        FileVersion: "0.0.1",
        ProductVersion:"0.0.0",
        InternalName:"Android apk Sideload Tool From Heizi Flash Tools",
        ProductName:"Android Package Sideload Tool",
        FileDescription:"线刷APK安装工具",
        LegalCopyright:"Github/ElisaMin",
        CompanyName:"Heizi"
    },jar:{
        files:["path/to/jar"],
        launcher:{
            file:(()=> {return this.files.at(0)})(),
            mainClass:"tools.heizi.ast.Main", // or null by manifest,
            args:{
                0:"-a",
                last:"-c"
            }//map
        }
    },jre:{
        search:[JvmDir("./lib/runtime"),EnviromentVar("JAVA_HOME")],
        options:[], // eg -Xmx256m
        nativeLibs:[],
        version:till(11,19), // kotlin 11..19 or Array<Float>
        preferred:DefaultVM // ClientHotpot, ServerHotspot,
    },splashScreen:ImageAndText("path/toImage",{
        startLine:{
            text:"welcome"
        },versionLine:null
    })
}
```
### vars.rs from https://github.com/ElisaMin/Jweust-template/tree/0.0.1 
```

// pub const INCLUDE_JAR:bool = false; // not support
pub const APPLICATION_WITH_OUT_CLI:Option<Option<&'static str>> = Some(Some("-DConsolog=true"));
pub const WORKDIR:Option<(&'static str,bool)> = None;
// pub const WORKDIR_IS_VARIABLE:bool = false;

pub const LOG_STDERR_PATH:Option<(Option<&'static str>,bool)> = None;
pub const LOG_STDOUT_PATH:Option<(Option<&'static str>,bool)> = None;

pub const CHARSET_STDOUT:Option<&'static str> = Some("GBK");
pub const CHARSET_JVM:Option<&'static str> = None;
pub const CHARSET_PAGE_CODE:Option<&'static str> = None;

pub const JAR_FILES:&[&str] = &["H:\\gits\\Heizi-Flashing-Tools\\tools\\sideload-install-wizard\\build\\libs\\sideload-install-wizard-0.0.9-all.jar"];
pub const JAR_LAUNCHER_FILE:usize = 0;
pub const JAR_LAUNCHER_MAIN_CLASS:Option<&str> = None;
pub const JAR_LAUNCHER_ARGS:&[(i32,&str)] = &[];

pub const EXE_IS_INSTANCE:bool = false;
pub const EXE_PERMISSION:i8 = -1;
pub const EXE_ICON_PATH:Option<&str> = Some("D:\\Downloads\\ic_ast_ugly.ico");
pub const EXE_FILE_VERSION:&str = "0.0.0.9";
pub const EXE_PRODUCT_VERSION:&str = "0.0.9";
pub const EXE_INTERNAL_NAME:&str = "Android apk Sideload Tool From Heizi Flash Tools";
pub const EXE_FILE_DESCRIPTION:&str = "线刷APK安装工具";
pub const EXE_LEGAL_COPYRIGHT:&str = "Github/ElisaMin";
pub const EXE_COMPANY_NAME:& str = "Heizi";

// new
pub const EXE_ARCH:&str = "x86_64";
pub const EXE_PRODUCT_NAME:&str = "Android Package Sideload Tool";
pub const RUST_PROJECT_NAME:&str = "sideload-install-wizard";

pub const JRE_SEARCH_DIR:&[&str] = &["./lib/runtime"];
pub const JRE_SEARCH_ENV:&[&str] = &["JAVA_HOME"];
pub const JRE_OPTIONS:&[&str] = &[];
pub const JRE_NATIVE_LIBS:&[&str] = &[];
pub const JRE_VERSION:&[&str] = &["19.0"];
pub const JRE_PREFERRED:& str = "DefaultVM";
pub const SPLASH_SCREEN_IMAGE_PATH:Option<&'static str> = Some("H:\\tupm\\ic_ast_ugly.png");
```
