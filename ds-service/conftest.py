import os
import sys

# Garante que `app` seja importável independente de como o pytest resolve o
# sys.path (roda tanto de dentro de ds-service/ quanto da raiz do repo).
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
