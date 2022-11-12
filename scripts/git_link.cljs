(ns git-link
  "git link 생성
  ## 참고
  - https://github.com/microsoft/vscode
  - https://github.com/BetterThanTomorrow/joyride 
  - https://github.com/sshaw/git-link 
  - https://github.com/gimenete/github-linker/blob/master/src/extension.ts 
 "
  (:require ["fs" :as fs]
            ["path" :as path]
            ["vscode" :as vsode]
            [clojure.string :refer [replace split-lines trim-newline]]
            [clojure.string :as str]
            [joyride.core :as joyride]
            [promesa.core :as p]))

(defn current-selection []
  (let [editor    ^js vscode/window.activeTextEditor
        selection (.-selection editor)]
    selection))

(defn current-document []
  (let [editor   ^js vscode/window.activeTextEditor
        document (.-document editor)]
    document))

(defn current-workspace []
  vscode/workspace.rootPath)

(defn file-path []
  (let [workspace (current-workspace)
        full-path (-> (current-document)
                      .-fileName)]
    (replace full-path workspace "")))

(defn line-number []
  [(-> (current-selection)
       .-start
       .-line)
   (-> (current-selection)
       .-end
       .-line)])

(defn current-branch
  "현재 브랜치 정보를 가져온다."
  []
  (let [head-content (-> (path/join vscode/workspace.rootPath
                                    ".git" "HEAD")
                         fs/readFileSync
                         (.toString "utf-8")
                         trim-newline)
        branch       (re-find #"[^/]*$" head-content)]
    branch))

(defn current-branch-ref []
  (let [head-content (-> (path/join vscode/workspace.rootPath
                                    ".git" "HEAD")
                         fs/readFileSync
                         (.toString "utf-8")
                         trim-newline)
        ref          (re-find #"\S*$" head-content)]
    ref))

(defn ref-sha []
  (let [ref (current-branch-ref)
        sha (-> (path/join vscode/workspace.rootPath
                           ".git" ref)
                fs/readFileSync
                (.toString "utf-8")
                trim-newline)]
    sha))

(defn host-url
  "git@github.com:jacegem/joyride-scripts.git
   https://github.com/jacegem/log.git"
  []
  (let [configs (-> (path/join vscode/workspace.rootPath ".git" "config")
                    fs/readFileSync
                    (.toString "utf-8")
                    trim-newline
                    split-lines)
        url     (->> configs
                     (filter #(re-find #"url = \w+" %))
                     first
                     (re-find  #"\S+$"))]
    (if (str/ends-with? url ".git")
      (->> (re-find #".*:(.*)\.git" url)
           second
           (str "https://github.com/"))
      url)))

(comment
  (host-url)
  (ref-sha)
  (current-branch-ref)
  :rcf)

(defn git-link []
  (let [host  (host-url)
        sha   (ref-sha)
        path  (file-path)
        lines (line-number)]
    (str host "/blob/" sha path "#L" (inc (first lines)) "#L" (inc (second lines)))))

(when (= (joyride/invoked-script) joyride/*file*)
  (p/do! (vscode/env.clipboard.writeText (git-link))))

(comment
  (git-link)
  (host-url)
  (ref-sha)
  (file-path)
  "https://github.com/jacegem/light-poly/blob/7b40329f459d8e2dc00ed2273d4b790b30cbe575/.joyride/scripts/git_link.cljs#L39"
  "https://github.com/jacegem/light-poly/blob/7b40329f459d8e2dc00ed2273d4b790b30cbe575/.joyride/scripts/git_link.cljs#L77#L77"
  #_{})
