(ns clojurewerkz.elastisch.native-api.indexing-test
  (:refer-clojure :exclude [replace])
  (:require [clojurewerkz.elastisch.native          :as es]
            [clojurewerkz.elastisch.native.document :as doc]
            [clojurewerkz.elastisch.native.index    :as idx]
            [clojurewerkz.elastisch.query           :as q]
            [clojurewerkz.elastisch.fixtures        :as fx]
            [clojurewerkz.elastisch.test.helpers    :as th]
            [clojurewerkz.elastisch.native.response :refer [hits-from any-hits? no-hits?]]
            [clojure.test :refer :all]
            [clj-time.core :refer [months ago]]
            [clojure.string :refer [join]]))

(def ^{:const true} index-name "people")
(def ^{:const true} index-type "person")

(th/maybe-connect-native-client)
(use-fixtures :each fx/reset-indexes)

;;
;; document/put
;;

(deftest ^{:indexing true :native true} test-sync-put-with-autocreated-index
  (let [id         "1"
        document   fx/person-jack
        response   (doc/put index-name index-type id document)
        get-result (doc/get index-name index-type id)]
    (is (idx/exists? index-name))
    (are [expected actual] (= expected (actual get-result))
         document   :_source
         index-name :_index
         index-type :_type
         id         :_id
         true       :exists)))

(deftest ^{:indexing true :native true} test-async-put-with-autocreated-index
  (let [id         "1"
        document   fx/person-jack
        response   (doc/put index-name index-type id document)
        get-result (doc/async-get index-name index-type id)]
    (is (idx/exists? index-name))
    (are [expected actual] (= expected (actual @get-result))
         document   :_source
         index-name :_index
         index-type :_type
         id         :_id
         true       :exists)))

(deftest ^{:indexing true :native true} test-sync-put-with-precreated-index-without-mapping-types
  (let [id         "1"
        _          (idx/create index-name)
        document   fx/person-jack
        response   (doc/put index-name index-type id document)
        get-result (doc/get index-name index-type id)]
    (are [expected actual] (= expected (actual get-result))
         document   :_source
         index-name :_index
         index-type :_type
         id         :_id
         true       :exists)))

(deftest ^{:indexing true :native true} test-sync-put-with-precreated-index-with-mapping-types
  (let [id         "1"
        _          (idx/create index-name :mappings fx/people-mapping)
        document   fx/person-jack
        response   (doc/put index-name index-type id document)
        get-result (doc/get index-name index-type id)]
    (are [expected actual] (= expected (actual get-result))
         document   :_source
         index-name :_index
         index-type :_type
         id         :_id
         true       :exists)))

(deftest ^{:indexing true :native true} test-sync-put-with-missing-document-versioning-type
  (let [id       "1"
        _        (doc/put index-name index-type id fx/person-jack)
        _        (doc/put index-name index-type id fx/person-mary)
        response (doc/put index-name index-type id fx/person-joe :version 1)]
    (is (:_index response))
    (is (:_type response))))

(deftest ^{:indexing true :native true} test-sync-put-with-conflicting-document-version
  (let [id       "1"
        _        (doc/put index-name index-type id fx/person-jack)
        _        (doc/put index-name index-type id fx/person-mary)
        response (doc/put index-name index-type id fx/person-joe :version 1 :version_type "external")]
    (is (:_index response))
    (is (:_type response))))

(deftest ^{:indexing true :native true} test-sync-put-with-new-document-version
  (let [id       "1"
        _        (doc/put index-name index-type id fx/person-jack :version 1 :version_type "external")
        _        (doc/put index-name index-type id fx/person-mary :version 2 :version_type "external")
        response (doc/put index-name index-type id fx/person-joe  :version 3 :version_type "external")]
    (is (= 3 (:_version response)))
    (is (:_index response))
    (is (:_type response))))

(deftest ^{:indexing true :native true} test-create-when-already-created
  (let [id       "1"
        _        (doc/put index-name index-type id fx/person-jack)
        response (doc/put index-name index-type id fx/person-joe :op_type "create")]
    (is (:_index response))
    (is (:_type response))))

(deftest ^{:indexing true :native true} test-sync-put-with-a-timestamp
  (let [id       "1"
        _        (idx/create index-name :mappings fx/people-mapping)
        response (doc/put index-name index-type id fx/person-jack :timestamp "2009-11-15T14:12:12")]
    (is (:_index response))
    (is (:_type response))))

(deftest ^{:indexing true :native true} test-sync-put-with-a-10-seconds-ttl
  (let [id       "1"
        _        (idx/create index-name :mappings fx/people-mapping)
        response (doc/put index-name index-type id fx/person-jack :ttl 10000)]
    (is (:_index response))
    (is (:_type response))))

(deftest ^{:indexing true :native true} test-sync-put-with-refresh-set-to-true
  (let [id       "1"
        _        (idx/create index-name :mappings fx/people-mapping)
        response (doc/put index-name index-type id fx/person-jack :refresh true)]
    (is (:_index response))
    (is (:_type response))))


;;
;; document/create
;;

(deftest ^{:indexing true :native true} test-put-create-autogenerate-id-test
  (let [response (doc/create index-name index-type fx/person-jack)]
    (is (:_id response))
    (are [expected actual] (= expected (actual response))
         index-name :_index
         index-type :_type
         1          :_version)))

