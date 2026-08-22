{{- define "mesh-lab-inventory.name" -}}
{{- default .Chart.Name .Values.app.name | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "mesh-lab-inventory.fullname" -}}
{{- printf "%s-%s" .Release.Name (include "mesh-lab-inventory.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "mesh-lab-inventory.labels" -}}
app.kubernetes.io/name: {{ include "mesh-lab-inventory.name" . }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version | replace "+" "_" }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/version: {{ .Values.app.version | quote }}
{{- end -}}

{{- define "mesh-lab-inventory.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "mesh-lab-inventory.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end -}}
