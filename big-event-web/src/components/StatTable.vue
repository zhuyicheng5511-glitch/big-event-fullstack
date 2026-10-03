<script setup>
defineProps({
  columns: { type: Array, required: true },
  data: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})
</script>

<template>
  <el-table :data="data" v-loading="loading" style="width: 100%">
    <el-table-column
      v-for="col in columns"
      :key="col.prop"
      :prop="col.prop"
      :label="col.label"
      :width="col.width"
    />
    <template #empty>
      <el-empty description="没有数据" />
    </template>
  </el-table>
</template>
