// Global Pipeline Library step: records artifact identity + source provenance
// so a Wiz-style digest/hash can be traced back to repo/commit/build later.
def call(Map args) {
    def digest = args.digest
    def name   = args.name ?: 'unnamed-artifact'
    def type   = args.type ?: 'unknown'

    if (!digest) {
        error "stampProvenance: 'digest' is required (image digest / Lambda hash / checksum)"
    }

    def commit = env.GIT_COMMIT ?: 'unknown'
    def repoUrl = env.GIT_URL ?: 'unknown'
    def buildUrl = env.BUILD_URL ?: 'unknown'
    def jobName = env.JOB_NAME ?: 'unknown'
    def buildNumber = env.BUILD_NUMBER ?: 'unknown'

    echo "Stamping provenance for artifact '${name}' (${type})"
    echo "  digest   = ${digest}"
    echo "  commit   = ${commit}"
    echo "  repo     = ${repoUrl}"
    echo "  build    = ${buildUrl}"

    // The fingerprinted file's content is EXACTLY the digest string.
    // Jenkins fingerprints by MD5(file content), so MD5(digest) becomes a stable,
    // globally-lookupable key: anyone holding the digest (e.g. from a Wiz finding)
    // can compute MD5(digest) and query /fingerprint/<md5>/api/json on this
    // controller to get back every build/job that produced or touched it.
    writeFile file: 'digest.txt', text: digest
    fingerprint 'digest.txt'

    // Human-readable record alongside it, for direct inspection in build artifacts.
    def manifest = """{
  "name": "${name}",
  "type": "${type}",
  "digest": "${digest}",
  "commit": "${commit}",
  "repositoryUrl": "${repoUrl}",
  "buildUrl": "${buildUrl}",
  "jobName": "${jobName}",
  "buildNumber": "${buildNumber}"
}
"""
    writeFile file: 'provenance.json', text: manifest
    archiveArtifacts artifacts: 'provenance.json', fingerprint: true

    return manifest
}
