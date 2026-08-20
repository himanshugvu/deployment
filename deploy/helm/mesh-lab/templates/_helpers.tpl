{{- define "mesh-lab.name" -}}
{{- default .Chart.Name .Values.app.name | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "mesh-lab.fullname" -}}
{{- printf "%s-%s" .Release.Name (include "mesh-lab.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "mesh-lab.labels" -}}
app.kubernetes.io/name: {{ include "mesh-lab.name" . }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version | replace "+" "_" }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/version: {{ .Values.app.version | quote }}
{{- end -}}
