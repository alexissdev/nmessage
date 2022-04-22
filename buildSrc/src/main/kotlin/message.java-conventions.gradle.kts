plugins {
  `java-library`
  `maven-publish`
}

repositories {
  mavenCentral()
}

dependencies {
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(8))
  }
}

val snapshotRepository: String by project
val releaseRepository: String by project

publishing {
  repositories {
    maven {
      url = if (project.version.toString().endsWith("-SNAPSHOT")) {
        uri(snapshotRepository)
      } else {
        uri(releaseRepository)
      }
      credentials {
        username = project.properties["UNNAMED_REPO_USER"] as String?
          ?: System.getenv("REPO_USER")
        password = project.properties["UNNAMED_REPO_PASSWORD"] as String?
          ?: System.getenv("REPO_PASSWORD")
      }
    }
  }
  publications {
    create<MavenPublication>("maven") {
      from(components["java"])
    }
  }
}

tasks {
  test {
    useJUnitPlatform()
  }
}