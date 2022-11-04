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

(defn fetch-head []
  (let [branch (current-branch)
        heads  (-> (path/join vscode/workspace.rootPath ".git" "FETCH_HEAD")
                   fs/readFileSync
                   (.toString "utf-8")
                   trim-newline
                   split-lines)]
    (->> heads
         (filter #(re-find (re-pattern (str "'" branch "'")) %))
         first)))

(defn branch-sha
  "현 브랜치의 sha 반환"
  []
  (->> (fetch-head)
       (re-find #"^[^\s]*")))

(defn append-github-url
  "두가지 경우
  a58d83947d3d1e77f887871d6d450fdb962740d2 branch 'main' of https://github.com/jacegem/vscode-user
  57a946b3db9e513e0cdbbb105d998ed7e7df8889 branch 'develop' of github.com:jacegem/light-poly "
  [text]
  (if (str/includes? text "//github.com/")
    (str "https:" text)
    (str "https://github.com/" text)))

(defn host-url
  []
  (->> (fetch-head)
       (re-find #"[^:]*$")
       append-github-url))

(comment
  (host-url)
  (str/replace "ABC" #"A" "X")
  (str/includes? "ABC" "A")
  #_{})

(defn git-link []
  (let [host  (host-url)
        sha   (branch-sha)
        path  (file-path)
        lines (line-number)]
    (str host "/blob/" sha path "#L" (inc (first lines)) "#L" (inc (second lines)))))

(when (= (joyride/invoked-script) joyride/*file*)
  (p/do! (vscode/env.clipboard.writeText (git-link))))

(comment
  (git-link)
  (host-url)
  (branch-sha)
  (file-path)
  "https://github.com/jacegem/light-poly/blob/7b40329f459d8e2dc00ed2273d4b790b30cbe575/.joyride/scripts/git_link.cljs#L39"
  "https://github.com/jacegem/light-poly/blob/7b40329f459d8e2dc00ed2273d4b790b30cbe575/.joyride/scripts/git_link.cljs#L77#L77"
  #_{})
