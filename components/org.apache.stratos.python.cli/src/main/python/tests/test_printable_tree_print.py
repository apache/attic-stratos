# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import sys
from cli import Utils


class TestClass:

    def __init__(self):
        pass

    @staticmethod
    def test_zero():
        i = 1
        assert i == 1

    @staticmethod
    def test_one():
        tree = Utils.PrintableTree(
            """[{"id":"network-partition-2","partitions":[{"id":"partition-2","partitionMax":0,"property":[{"name":"region","value":"default"}],"public":false},{"id":"partition-3","partitionMax":0,"property":[{"name":"region","value":"default"}],"public":false}]},{"id":"network-partition-1","partitions":[{"id":"partition-1","partitionMax":0,"property":[{"name":"region","value":"default"}],"public":false}]}]""")
        tree.print_tree()
        output = sys.stdout.getline().strip()  # because stdout is an StringIO instance
        assert output == 'hello world!'
