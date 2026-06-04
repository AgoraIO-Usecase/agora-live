import os, sys

def modfiy(path, isReset):
    appId = os.environ.get('APP_ID')
    cert = os.environ.get('APP_CERT')
    im_app_key = os.environ.get('IM_APP_KEY', '')
    manifest_url = os.environ.get('manifest_url', '')
    with open(path, 'r', encoding='utf-8') as file:
        contents = []
        for num, line in enumerate(file):
            line = line.strip()
            if "static let AppId" in line:
                if isReset:
                    line = "static let AppId: String = <#YOUR APPID#>"
                else:
                    line = f'static let AppId: String = "{appId}"'
            
            elif "static var IMAppKey" in line:
                if isReset:
                    line = "static var IMAppKey: String? = <#YOUR IMAppKey#>"
                else:
                    line = f'static var IMAppKey: String? = "{im_app_key}"' if im_app_key else "static var IMAppKey: String? = nil"

            elif "static var DynamicResourceUrl" in line:
                if isReset:
                    line = 'static var DynamicResourceUrl: String? = ""'
                else:
                    line = f'static var DynamicResourceUrl: String? = "{manifest_url}"'

            contents.append(line)
        file.close()
        
        with open(path, 'w', encoding='utf-8') as fw:
            for content in contents:
                if "{" in content or "}" in content:
                    fw.write(content + "\n")
                else:
                    fw.write('\t'+content + "\n")
            fw.close()


if __name__ == '__main__':
    print(f'argv === {sys.argv[1:]}')
    path = sys.argv[1:][0]
    isReset = eval(sys.argv[1:][1])
    modfiy(path.strip(), isReset)
