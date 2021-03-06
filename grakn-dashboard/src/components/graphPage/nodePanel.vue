<!--
Grakn - A Distributed Semantic Database
Copyright (C) 2016  Grakn Labs Limited

Grakn is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Grakn is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Grakn. If not, see <http://www.gnu.org/licenses/gpl.txt>.
-->

<template>
<div class="node-panel z-depth-1-half" v-show="showNodePanel">
    <div class="panel-heading">
        <h4 style="word-break: normal;" class="noselect"><i id="graph-icon" class="pe page-header-icon pe-7s-share"></i>{{nodeLabel}}</h4>
        <div class="panel-tools">
            <a @click="closePanel" class="panel-close"><i class="fa fa-times"></i></a>
        </div>
    </div>
    <div class="panel-body">
        <div class="dd-header">Node:</div>
        <div class="node-properties">
            <div class="dd-item" v-for="(value, key) in nodeProperties">
                <div><span class="list-key">{{key}}:</span> {{value}}</div>
            </div>
        </div>
        <div class="dd-header" v-show="Object.keys(nodeResources).length">Resources:</div>
        <div class="dd-item" v-for="(value,key) in nodeResources">
            <div class="dd-handle noselect" @dblclick="addResourceNodeWithOwners(value.link)"><span class="list-key">{{key}}:</span>
                <a v-if="value.href" :href="value.label" style="word-break: break-all; color:#00eca2;" target="_blank">{{value.label}}</a>
                <span v-else> {{value.label}}</span>
            </div>
        </div>
        <div class="dd-header" v-show="Object.keys(nodeLinks).length">Links:</div>
        <div class="dd-item" v-for="(value, key) in nodeLinks">
            <div class="dd-handle"><span class="list-key">{{key}}:</span> {{value}}</div>
        </div>
    </div>
</div>
</template>

<style scoped>
.node-panel {
    z-index: 2;
    position: absolute;
    left: 80%;
    top: 20px;
    width: 270px;
    background-color: #0f0f0f;
    padding: 10px;
    max-height: 95%;
    overflow: scroll;
    user-select: none;
    -moz-user-select: none;
    -ms-overflow-style: none;
    overflow: -moz-scrollbars-none;
}

.node-panel::-webkit-scrollbar {
    display: none;
}

.panel-heading {
    cursor: move;
    color: white;
    font-weight: 500;
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    font-size:110%;
    margin-bottom: 10px;
}

.panel-close{
  cursor: default;
}

.fa-times{
  cursor: pointer;
}

.dd-header {
    margin-top: 10px;
    margin-bottom: 5px;
}

.dd-handle {
    cursor: pointer;
    padding: 10px 10px;
    border-radius: 3px;
    margin: 3px 0px;
}

.node-properties {
    cursor: text;
    padding: 10px 10px;
    border-radius: 3px;
}

.node-panel .panel-body {
    overflow: scroll;
}

.node-panel .panel-body::-webkit-scrollbar {
    display: none;
}

.list-key {
    font-weight: bold;
    color: #FFFFFF;
}

h4{
  font-weight: 700;
}
</style>

<script>
/* @flow */

import { URL_REGEX } from '../../js/HAL/HALParser';

export default {
  name: 'NodePanel',
  props: ['node', 'showNodePanel'],
  data() {
    return {
    };
  },
  created() {
  },
  mounted() {
    this.$nextTick(() => {
      new Draggabilly(document.querySelector('.node-panel'), { handle: '.panel-heading', containment: '.graph-panel-body' });
    });
  },
  computed: {
    nodeResources() {
      if (this.node === undefined) return {};
      return this.prepareResources(this.node.properties);
    },
    nodeProperties() {
      if (this.node === undefined) return {};
      return {
        id: this.node.id,
        type: this.node.type,
        baseType: this.node.baseType,
      };
    },
    nodeLinks() {
      return {};
    },
    nodeLabel() {
      if (this.node === undefined) return {};
      return visualiser.getNodeLabel(this.node.id);
    },
  },
  methods: {
    addResourceNodeWithOwners(resourceId) {
      this.$emit('load-resource-owners', resourceId);
    },
    closePanel() {
      this.$emit('close-node-panel');
    },
  /**
   * Prepare the list of resources to be shown in the right div panel
   * It sorts them alphabetically and then check if a resource value is a URL
   */
    prepareResources(originalObject) {
      if (originalObject == null) return {};
      return Object.keys(originalObject).sort().reduce(
          // sortedObject is the accumulator variable, i.e. new object with sorted keys
          // k is the current key
          (sortedObject, k) => {
              // Add 'href' field to the current object, it will be set to TRUE if it contains a valid URL, FALSE otherwise
            const currentResourceWithHref = Object.assign(originalObject[k], { href: this.validURL(originalObject[k].label) });
            return Object.assign(sortedObject, { [k]: currentResourceWithHref });
          }, {});
    },
    validURL(str) {
      const pattern = new RegExp(URL_REGEX, 'i');
      return pattern.test(str);
    },

  },
};
</script>
