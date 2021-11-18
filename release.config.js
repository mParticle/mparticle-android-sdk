module.exports = {
  branches: ["main"],
  tagFormat: "v${version}",
  plugins: [
    [
      "@semantic-release/commit-analyzer",
      {
        preset: "angular",
        releaseRules: [
            {type: 'feat', release: 'minor'},
            {type: 'ci', release: 'patch'},
            {type: 'fix', scope: 'patch'},
            {type: 'docs', release: 'patch'},
            {type: 'test', release: 'patch'},
            {type: 'refactor', release: 'patch'},
            {type: 'style', release: 'patch'},
            {type: 'build', release: 'patch'},
            {type: 'chore', release: 'patch'},
            {type: 'revert', release: 'patch'}
       ]
      }
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
