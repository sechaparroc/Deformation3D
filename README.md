# Deformation3D
Implementation of different deformation methods (3D) using Processing and Proscene.

Affine Transformations using MLS and Rigid Transformations using Laplacian Operator

This is an implementation in Processing of:

* Affine transformations using MLS as is proposed on: 
  http://www.cs.rice.edu/~jwarren/research/mls.pdf
* Rigid transformations using Laplacian coordinates as is proposed on:
  https://igl.ethz.ch/projects/Laplacian-mesh-processing/Laplacian-mesh-editing/laplacian-mesh-editing.pdf

So, the idea is that a mesh (.obj model) is loaded and, basically, you can add, remove and traslate control points and drag them to deform the shape.

The Scene that is shown is composed by two models, one is the representation of the original model, the other one is going to show the deformations suffered by the model according to the handler points.

DEMO: http://nakednous.github.io/proscene3_design/vid/deformation.mp4
