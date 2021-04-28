
def downloadArtifact(String nexusRepo, String artifactId, String path){
    def slurper = new groovy.json.JsonSlurper()
    def cmd = ['bash', '-c', "curl -u admin:admin123 -X GET \"http://192.168.161.128:8081/service/rest/v1/search/assets?repository=$nexusRepo&name=$artifactId\" --noproxy '*' -H \"accept: application/json\""]
    def result = cmd.execute().text
    def json = slurper.parseText(result)

    def items = json.items
    for(i in items){
        if(getJarName(path.trim()) == getJarName(i.get("path")) ) {
            return i.get("downloadUrl")
        }
    }
}

def getJarName(String path){
    def fpath
    if(path!=null) {
        fpath = path.substring(path.lastIndexOf('/') + 1)
    }
    else
        fpath = ''
    return fpath
}

import groovy.json.JsonSlurper
def getArtficatsList(String artifactsURL){
    try {
        List<String> artifacts = new ArrayList<String>()
        def artifactsObjectRaw = ["curl", "-s", "-H", "accept: application/json", "-k", "--url", "${artifactsURL}"].execute().text
        def jsonSlurper = new JsonSlurper()
        def artifactsJsonObject = jsonSlurper.parseText(artifactsObjectRaw)
        def dataArray = artifactsJsonObject.items
        for(item in dataArray){
            artifacts.add(item.downloadUrl)
        }
        def flt = artifacts.findAll{
            !it.contains(".md5") && !it.contains(".sha1") && !it.contains(".xml")
        }
        print "=>>>>"+flt.reverse()
        return flt.reverse()
    } catch (Exception e) {
        print "There was a problem fetching the artifacts"
    }


}

return this
