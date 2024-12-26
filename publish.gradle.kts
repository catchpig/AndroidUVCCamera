plugins {
    id("maven-publish")
}

publishing {
    publications {
        create<MavenPublication>("jitpack") {
            if (project.plugins.hasPlugin("com.android.library")) {
                from(components["release"])
//                            artifact androidSourcesJar
            } else if (project.plugins.hasPlugin("java-library")) {
                from(components["java"])
//                    artifact javadocJar
//                            artifact javaSourcesJar
            }
            val projectName = project.name
            groupId = 'com.github.catchpig.kmvvm'
            artifactId = projectName
            version = "1.0.0"
        }
    }
    repositories {
        maven {
            name = "local"
            url = uri("${rootProject.buildDir}")
        }
    }
}