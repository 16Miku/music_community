plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = com.example.music_community" | Set-Content -Path replacements.txt"
    compileSdk = 35

    defaultConfig {
        applicationId = com.example.music_community" | Set-Content -Path replacements.txt"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)


    // 显式添加 androidx.core 依赖，确保所有 AndroidX 兼容性
    implementation("androidx.core:core-ktx:1.10.0") // 可以使用最新稳定版本


    // **新增：解决 NestedScrollingParent 错误，提供旧Support Library兼容性**
    implementation("androidx.legacy:legacy-support-v4:1.0.0")






    // 添加 RecyclerView 依赖，如果您的项目中还没有
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // SmartRefreshLayout 依赖
    implementation("io.github.scwang90:refresh-layout-kernel:2.1.0") // 核心库
    implementation("io.github.scwang90:refresh-header-classics:2.1.0") // 经典刷新头
    implementation("io.github.scwang90:refresh-footer-classics:2.1.0") // 经典加载尾

    // BaseRecyclerViewAdapterHelper (BRVAH) 核心库
    // 从v4开始，本库将会上传至maven中央仓库，不需要再添加三方仓库配置了。
    // 请访问 BRVAH GitHub 仓库 (https://github.com/CymChad/BaseRecyclerViewAdapterHelper) 获取最新稳定版本和确认正确的依赖引入方式
//    implementation ("io.github.cymchad:BaseRecyclerViewAdapterHelper4:4.1.4")

    // BaseRecyclerViewAdapterHelper (BRVAH) 依赖 - **已升级到 3.0.10**
    // 3.x 版本对 AndroidX 兼容性更好，推荐使用
    // 将JitPack存储库添加到您的项目构建文件settings.gradle.kts中,RecyclerViewAdapter的3.0.13 及以后版本不再需要
    implementation("com.github.CymChad:BaseRecyclerViewAdapterHelper:3.0.10")
//    implementation("io.github.cymchad:BaseRecyclerViewAdapterHelper:3.0.14")


    // Glide 图片加载库依赖
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Glide 图片转换库，用于模糊、圆角等效果
    implementation("jp.wasabeef:glide-transformations:4.3.0")


    // CardView 依赖
    implementation("androidx.cardview:cardview:1.0.0")


    // Glide 注解处理器，用于生成 GlideApp 类，提供更好的API体验和自定义模块支持
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")




    // RecyclerViewPreloader 是 Glide 官方提供的集成库，用于优化 RecyclerView 滚动性能
    implementation("com.github.bumptech.glide:recyclerview-integration:4.16.0")



    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0") // 新增日志拦截器依赖



    // 【新增】MediaSession 依赖
    implementation("androidx.media:media:1.7.0") // 可以使用最新稳定版本


    // 【新增】用于 Palette 库，提取图片主题色 (为后续步骤做准备，现在添加)
    implementation("androidx.palette:palette:1.0.0")



}