module.exports = {
  branches: ["master"],
  tagFormat: "v${version}",
  plugins: [
    [
      "@semantic-release/commit-analyzer",
      {
        preset: "angular",
      },
    ],
    [
      "@semantic-release/release-notes-generator",
      {
        preset: "angular",
      },
    ],
    [
      "@semantic-release/changelog",
      {
        changelogFile: "CHANGELOG.md",
      },
    ],
    [
      "@semantic-release/exec",
      {
        prepareCmd: "sh ./scripts/release.sh ${nextRelease.version}",
      },
    ],
    ["@semantic-release/github"],
    [
      "@semantic-release/git",
      {
        assets: ["CHANGELOG.md", "build.gradle", "README.md"],
        message:
          "chore(release): ${nextRelease.version} [skip ci]\n\n${nextRelease.notes}",
      },
    ],
  ],
};
